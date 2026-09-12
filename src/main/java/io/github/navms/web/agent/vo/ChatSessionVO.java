package io.github.navms.web.agent.vo;

import java.time.LocalDateTime;

/**
 * 会话摘要。
 *
 * @param id        会话主键
 * @param title     标题
 * @param status    active / interrupted
 * @param updatedAt 最近更新时间
 * @author navms
 */
public record ChatSessionVO(Long id, String title, String status, LocalDateTime updatedAt) {
}
