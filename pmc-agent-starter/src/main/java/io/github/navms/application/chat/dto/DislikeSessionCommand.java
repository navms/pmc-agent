package io.github.navms.application.chat.dto;

/**
 * 将会话记为 Langfuse base case（点踩）。
 *
 * @param sessionId 会话 ID
 * @param messageId 被踩的助手消息协议 ID
 * @param rating    目前仅支持 down
 * @author navms
 */
public record DislikeSessionCommand(Long sessionId, String messageId, String rating) {
}
