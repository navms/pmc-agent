package io.github.navms.agent.clarify;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.github.navms.agent.clarify.IntentClarifyParser.HistoryTurn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 用独立 ReActAgent 做意图分类（框架结构化输出）。
 *
 * @author navms
 */
@Slf4j
@Component
public class IntentClarifyService {

    private static final Duration TIMEOUT = Duration.ofSeconds(120);

    private final ReActAgent intentClarifyAgent;

    public IntentClarifyService(ReActAgent intentClarifyAgent) {
        this.intentClarifyAgent = intentClarifyAgent;
    }

    /**
     * @param currentUserText 本轮用户原文
     * @param history         会话历史（不含本轮）
     * @param context         当前运行时上下文，供 Langfuse session 绑定
     * @return 分类结果；失败为空
     */
    public Optional<IntentClarifyResult> classify(
            String currentUserText, List<HistoryTurn> history, RuntimeContext context) {
        if (!StringUtils.hasText(currentUserText)) {
            return Optional.empty();
        }
        String userPayload = """
                历史对话：
                %s

                当前用户消息：
                %s
                """.formatted(HistoryTurn.join(history), currentUserText.trim());
        Msg user = Msg.builder().role(MsgRole.USER).name("user").textContent(userPayload).build();
        try {
            Msg reply = intentClarifyAgent
                    .call(List.of(user), IntentClarifyResult.class, context)
                    .block(TIMEOUT);
            if (reply == null || !reply.hasStructuredData()) {
                return Optional.empty();
            }
            IntentClarifyResult result = reply.getStructuredData(IntentClarifyResult.class);
            if (result == null || !result.valid()) {
                return Optional.empty();
            }
            return Optional.of(result);
        } catch (Exception e) {
            log.warn("Intent clarify model call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
