package io.github.navms.application.chat.dto;

/**
 * 重命名会话。
 *
 * @param sessionId 会话 ID
 * @param title     新标题
 * @author navms
 */
public record RenameSessionCommand(Long sessionId, String title) {
}
