package io.github.navms.domain.chat.repository;

import io.github.navms.domain.chat.entity.ChatMessage;

import java.util.List;
import java.util.Optional;

/**
 * 消息仓储。
 *
 * @author navms
 */
public interface ChatMessageRepository {

    /**
     * 插入消息。
     *
     * @param message 消息
     * @return 带主键的消息
     */
    ChatMessage insert(ChatMessage message);

    /**
     * 按会话列出消息，序号升序。
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<ChatMessage> listBySessionId(Long sessionId);

    /**
     * 会话中序号最大的一条消息。
     *
     * @param sessionId 会话 ID
     * @return 最后一条消息
     */
    Optional<ChatMessage> findLastBySessionId(Long sessionId);
}
