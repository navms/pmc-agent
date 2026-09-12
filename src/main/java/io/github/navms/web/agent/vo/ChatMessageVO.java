package io.github.navms.web.agent.vo;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 历史消息。
 *
 * @param id          消息 ID
 * @param messageType 消息类型
 * @param content     展示文本
 * @param payload     完整结构
 * @param createdAt   创建时间
 * @author navms
 */
public record ChatMessageVO(
        Long id,
        String messageType,
        String content,
        Map<String, Object> payload,
        LocalDateTime createdAt
) {
}
