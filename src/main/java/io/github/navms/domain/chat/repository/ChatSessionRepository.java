package io.github.navms.domain.chat.repository;

import io.github.navms.domain.chat.entity.ChatSession;
import io.github.navms.domain.chat.valueobj.UserId;

import java.util.List;

/**
 * 会话仓储。
 *
 * @author navms
 */
public interface ChatSessionRepository {

    /**
     * 插入新会话。
     *
     * @param session 会话
     * @return 带主键的会话
     */
    ChatSession insert(ChatSession session);

    /**
     * 按主键加载，不存在则抛业务异常。
     *
     * @param sessionId 会话 ID
     * @return 会话
     */
    ChatSession requireById(Long sessionId);

    /**
     * 按用户列出会话，最近更新优先。
     *
     * @param userId 用户
     * @return 会话列表
     */
    List<ChatSession> listByUserId(UserId userId);

    /**
     * 按当前领域状态更新会话。
     *
     * @param session 会话
     */
    void update(ChatSession session);

    /**
     * 逻辑删除。
     *
     * @param sessionId 会话 ID
     */
    void deleteById(Long sessionId);
}
