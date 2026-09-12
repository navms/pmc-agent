package io.github.navms.infrastructure.chat.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.navms.domain.chat.entity.ChatSession;
import io.github.navms.domain.chat.exception.BusinessException;
import io.github.navms.domain.chat.exception.ChatErrorCode;
import io.github.navms.domain.chat.repository.ChatSessionRepository;
import io.github.navms.domain.chat.valueobj.UserId;
import io.github.navms.infrastructure.chat.converter.ChatSessionConverter;
import io.github.navms.infrastructure.chat.mapper.ChatSessionMapper;
import io.github.navms.infrastructure.chat.model.ChatSessionDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 会话仓储实现。
 *
 * @author navms
 */
@Repository
@RequiredArgsConstructor
public class ChatSessionRepositoryImpl implements ChatSessionRepository {

    private final ChatSessionMapper sessionMapper;

    @Override
    public ChatSession insert(ChatSession session) {
        ChatSessionDO row = ChatSessionConverter.INSTANCE.toChatSessionDO(session);
        sessionMapper.insert(row);
        return ChatSessionConverter.INSTANCE.toChatSession(row);
    }

    @Override
    public ChatSession requireById(Long sessionId) {
        ChatSessionDO row = sessionId == null ? null : sessionMapper.selectById(sessionId);
        if (row == null) {
            throw new BusinessException(ChatErrorCode.SESSION_NOT_FOUND);
        }
        return ChatSessionConverter.INSTANCE.toChatSession(row);
    }

    @Override
    public List<ChatSession> listByUserId(UserId userId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<ChatSessionDO>()
                        .eq(ChatSessionDO::getUserId, userId.value())
                        .orderByDesc(ChatSessionDO::getUpdatedAt))
                .stream()
                .map(ChatSessionConverter.INSTANCE::toChatSession)
                .toList();
    }

    @Override
    public void update(ChatSession session) {
        ChatSessionDO row = ChatSessionConverter.INSTANCE.toChatSessionDO(session);
        sessionMapper.update(null, new LambdaUpdateWrapper<ChatSessionDO>()
                .eq(ChatSessionDO::getId, row.getId())
                .set(ChatSessionDO::getTitle, row.getTitle())
                .set(ChatSessionDO::getStatus, row.getStatus())
                .set(ChatSessionDO::getUpdatedAt, row.getUpdatedAt()));
    }

    @Override
    public void deleteById(Long sessionId) {
        sessionMapper.deleteById(sessionId);
    }
}
