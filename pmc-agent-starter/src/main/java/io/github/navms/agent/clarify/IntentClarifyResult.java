package io.github.navms.agent.clarify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 意图澄清器结构化输出。
 *
 * @param status         ready 或 need_clarify
 * @param intent         query / summarize / export / chart / write / chitchat
 * @param enhancedPrompt 自洽任务描述
 * @param question       追问句
 * @param missingSlots   缺失槽位
 * @author navms
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IntentClarifyResult(
        @JsonProperty("status") String status,
        @JsonProperty("intent") String intent,
        @JsonProperty("enhancedPrompt") String enhancedPrompt,
        @JsonProperty("question") String question,
        @JsonProperty("missingSlots") List<String> missingSlots) {

    /**
     * @return 可直接路由
     */
    public boolean ready() {
        return "ready".equalsIgnoreCase(status);
    }

    /**
     * @return 需要向用户追问
     */
    public boolean needClarify() {
        return "need_clarify".equalsIgnoreCase(status);
    }

    /**
     * @return 字段齐全且 status 合法
     */
    public boolean valid() {
        return StringUtils.hasText(status)
                && StringUtils.hasText(intent)
                && (ready() || needClarify());
    }
}
