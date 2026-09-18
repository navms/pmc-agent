package io.github.navms.agent.clarify;

import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 将分类结果改写成 supervisor 可读的用户消息。
 *
 * @author navms
 */
public final class IntentClarifyParser {

    /**
     * @param original 用户原话
     * @param result   分类结果
     * @return 交给 supervisor 的 user 文本
     */
    public static String rewriteUserMessage(String original, IntentClarifyResult result) {
        if (result.needClarify()) {
            String question = StringUtils.hasText(result.question()) ? result.question().trim() : "请补充查询所缺的账号或时间范围。";
            return """
                    【意图澄清】请不要调用任何工具、不要 spawn 子 Agent。用自然语言向用户追问下面这个问题，一次只问一个：
                    %s
                    用户原话：%s
                    """.formatted(question, original);
        }
        String enhanced = StringUtils.hasText(result.enhancedPrompt()) ? result.enhancedPrompt().trim() : original;
        return """
                【已澄清任务】
                意图：%s
                目标：%s
                补全后的任务：%s
                用户原话：%s
                请严格按该意图执行：query→query_bank，summarize→summarize_bank，export→export_excel，chart→create_chart，write→直接调写工具，chitchat→只闲聊不 spawn。
                """.formatted(result.intent(), routeOf(result.intent()), enhanced, original);
    }

    /**
     * @param intent 分类意图
     * @return 路由说明
     */
    public static String routeOf(String intent) {
        if (intent == null) {
            return "未知";
        }
        return switch (intent.trim().toLowerCase()) {
            case "query" -> "query_bank";
            case "summarize" -> "summarize_bank";
            case "export" -> "export_excel";
            case "chart" -> "create_chart";
            case "write" -> "写工具";
            case "chitchat" -> "不 spawn";
            default -> intent;
        };
    }

    /**
     * 会话历史一行。
     *
     * @param role user / assistant
     * @param text 正文
     */
    public record HistoryTurn(String role, String text) {
        /**
         * @return 可送给模型的一行
         */
        public String line() {
            return role + ": " + text;
        }

        /**
         * @param turns 历史
         * @return 拼接文本
         */
        public static String join(List<HistoryTurn> turns) {
            if (turns == null || turns.isEmpty()) {
                return "(无)";
            }
            StringBuilder builder = new StringBuilder();
            for (HistoryTurn turn : turns) {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append(turn.line());
            }
            return builder.toString();
        }
    }
}
