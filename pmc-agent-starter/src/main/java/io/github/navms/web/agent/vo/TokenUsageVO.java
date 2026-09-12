package io.github.navms.web.agent.vo;

import org.springframework.ai.chat.metadata.Usage;

/**
 * 模型 Token 用量。
 *
 * @param promptTokens     输入 token
 * @param completionTokens 输出 token
 * @param totalTokens      合计
 * @author navms
 */
public record TokenUsageVO(Integer promptTokens, Integer completionTokens, Integer totalTokens) {

    /**
     * 从 Spring AI {@link Usage} 映射；全空则返回 null。
     *
     * @param usage 框架用量
     * @return VO，或 null
     */
    public static TokenUsageVO from(Usage usage) {
        if (usage == null) {
            return null;
        }
        Integer promptTokens = usage.getPromptTokens();
        Integer completionTokens = usage.getCompletionTokens();
        Integer totalTokens = usage.getTotalTokens();
        if (promptTokens == null && completionTokens == null && totalTokens == null) {
            return null;
        }
        return new TokenUsageVO(promptTokens, completionTokens, totalTokens);
    }
}
