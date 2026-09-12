package io.github.navms.application.chat.dto;

import java.time.LocalDateTime;

/**
 * 会话摘要。
 *
 * @param id        会话主键
 * @param title     标题
 * @param status    状态码
 * @param updatedAt 最近更新时间
 * @author navms
 */
public record ChatSessionInfo(Long id, String title, String status, LocalDateTime updatedAt) {
}
