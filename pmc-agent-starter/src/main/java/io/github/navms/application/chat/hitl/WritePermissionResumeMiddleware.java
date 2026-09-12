package io.github.navms.application.chat.hitl;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.agui.adapter.AguiAgentAdapter;
import io.agentscope.core.agui.model.AguiResume;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * AG-UI resume 时把 approved 转为父 Agent 的 ConfirmResult，继续当前会话。
 *
 * @author navms
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WritePermissionResumeMiddleware implements MiddlewareBase {

    private final WritePermissionHitlStore store;

    @Override
    public Flux<AgentEvent> onAgent(
            Agent agent,
            RuntimeContext context,
            AgentInput input,
            Function<AgentInput, Flux<AgentEvent>> next) {
        Boolean approved = extractApproved(context);
        if (approved == null || !StringUtils.hasText(context.getSessionId())) {
            return next.apply(input);
        }
        WritePermissionHitlStore.PendingConfirm pending = store.peek(context.getSessionId());
        if (pending == null || pending.toolCalls().isEmpty()) {
            return next.apply(input);
        }
        store.take(context.getSessionId());
        List<ConfirmResult> confirmResults = new ArrayList<>(pending.toolCalls().size());
        for (ToolUseBlock toolCall : pending.toolCalls()) {
            confirmResults.add(new ConfirmResult(approved, toolCall));
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(Msg.METADATA_CONFIRM_RESULTS, confirmResults);
        if (StringUtils.hasText(pending.replyId())) {
            metadata.put(Msg.METADATA_CONFIRM_REQUEST_REPLY_ID, pending.replyId());
        }
        Msg resumeMsg = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .textContent(approved ? "approved" : "denied")
                .metadata(metadata)
                .build();
        log.info(
                "Resuming parent write permission HITL: thread={}, approved={}, tools={}",
                context.getSessionId(),
                approved,
                confirmResults.size());
        return next.apply(new AgentInput(List.of(resumeMsg)));
    }

    private static Boolean extractApproved(RuntimeContext context) {
        if (context == null) {
            return null;
        }
        Object resume = context.get(AguiAgentAdapter.RUNTIME_CONTEXT_RESUME_KEY);
        if (!(resume instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Boolean approved = null;
        for (Object item : list) {
            if (!(item instanceof AguiResume aguiResume) || !aguiResume.isResolved()) {
                continue;
            }
            Object payload = aguiResume.getPayload();
            boolean value = false;
            if (payload instanceof Map<?, ?> map) {
                Object raw = map.get("approved");
                value = Boolean.TRUE.equals(raw) || "true".equalsIgnoreCase(String.valueOf(raw));
            } else if (payload instanceof Boolean bool) {
                value = bool;
            }
            approved = approved == null ? value : (approved || value);
        }
        return approved;
    }
}
