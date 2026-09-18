package io.github.navms.web.agent.param;

/**
 * 会话反馈（点踩）。
 *
 * @param messageId 被踩的助手消息协议 ID
 * @param rating    目前仅支持 down
 * @author navms
 */
public record SessionFeedbackRequest(String messageId, String rating) {
}
