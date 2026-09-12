package io.github.navms.application.chat.dto;

/**
 * 流式对话。
 *
 * @param sessionId 会话 ID
 * @param userId    用户标识
 * @param prompt    用户输入
 * @author navms
 */
public record StreamChatCommand(Long sessionId, String userId, String prompt) {
}
