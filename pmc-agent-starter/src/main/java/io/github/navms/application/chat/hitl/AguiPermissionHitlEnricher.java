package io.github.navms.application.chat.hitl;

import io.agentscope.core.agui.adapter.strategy.AguiEventEnricher;
import io.agentscope.core.agui.adapter.strategy.AguiStreamContext;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.message.ToolUseBlock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将写工具 RequireUserConfirm 转为 AG-UI interrupt，并缓存 pending（方案 A：父 Agent ASK）。
 *
 * @author navms
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AguiPermissionHitlEnricher implements AguiEventEnricher {

    /** 与前端 resume payload `{ approved }` 对齐；不可传 JSON null（Zod optional 不接受 null）。 */
    private static final Map<String, Object> APPROVED_RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of("approved", Map.of("type", "boolean")),
            "required", List.of("approved"));

    private static final Duration INTERRUPT_TTL = Duration.ofHours(24);

    private final WritePermissionHitlStore writePermissionHitlStore;

    @Override
    public List<AguiEvent> enrich(AgentEvent source, List<AguiEvent> events, AguiStreamContext context) {
        if (!(source instanceof RequireUserConfirmEvent requireConfirm) || context == null) {
            return events == null ? List.of() : events;
        }
        List<ToolUseBlock> toolCalls = requireConfirm.getToolCalls();
        if (toolCalls == null || toolCalls.isEmpty()) {
            return events == null ? List.of() : events;
        }
        String threadId = context.getThreadId();
        writePermissionHitlStore.save(threadId, requireConfirm.getReplyId(), toolCalls);
        String expiresAt = Instant.now().plus(INTERRUPT_TTL).toString();
        for (ToolUseBlock toolCall : toolCalls) {
            String toolCallId = toolCall.getId();
            if (!StringUtils.hasText(toolCallId)) {
                continue;
            }
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("toolName", toolCall.getName());
            if (toolCall.getInput() != null && !toolCall.getInput().isEmpty()) {
                metadata.put("toolInput", toolCall.getInput());
            }
            metadata.put("agentscope.interruptKind", "permission_confirm");
            if (StringUtils.hasText(requireConfirm.getReplyId())) {
                metadata.put("replyId", requireConfirm.getReplyId());
            }
            String message = StringUtils.hasText(toolCall.getName())
                    ? "请求执行 " + toolCall.getName()
                    : "请求执行写操作";
            context.addInterrupt(new AguiEvent.Interrupt(
                    toolCallId,
                    "tool_call",
                    message,
                    toolCallId,
                    APPROVED_RESPONSE_SCHEMA,
                    expiresAt,
                    metadata));
        }
        log.debug("Parent write permission interrupt(s) registered for thread={}", threadId);
        return events == null ? List.of() : events;
    }
}
