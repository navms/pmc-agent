package io.github.navms.application.chat.dto;

/**
 * 创建会话。
 *
 * @param userId 用户标识
 * @author navms
 */
public record CreateSessionCommand(String userId) {
}
