package io.github.navms.infrastructure.chat.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.navms.domain.chat.entity.ChatMessage;
import io.github.navms.domain.chat.repository.ChatMessageRepository;
import io.github.navms.infrastructure.chat.converter.ChatMessageConverter;
import io.github.navms.infrastructure.chat.mapper.ChatMessageMapper;
import io.github.navms.infrastructure.chat.model.ChatMessageDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 消息仓储实现。
 *
 * @author navms
 */
@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepository {

    private final ChatMessageMapper messageMapper;

    @Override
    public ChatMessage insert(ChatMessage message) {
        ChatMessageDO row = ChatMessageConverter.INSTANCE.toChatMessageDO(message);
        messageMapper.insert(row);
        return ChatMessageConverter.INSTANCE.toChatMessage(row);
    }

    @Override
    public List<ChatMessage> listBySessionId(Long sessionId) {
        return messageMapper.selectList(new LambdaQueryWrapper<ChatMessageDO>()
                        .eq(ChatMessageDO::getSessionId, sessionId)
                        .orderByAsc(ChatMessageDO::getSeq))
                .stream()
                .map(ChatMessageConverter.INSTANCE::toChatMessage)
                .toList();
    }

    @Override
    public Optional<ChatMessage> findLastBySessionId(Long sessionId) {
        ChatMessageDO last = messageMapper.selectOne(new LambdaQueryWrapper<ChatMessageDO>()
                .eq(ChatMessageDO::getSessionId, sessionId)
                .orderByDesc(ChatMessageDO::getSeq)
                .last("LIMIT 1"));
        return Optional.ofNullable(ChatMessageConverter.INSTANCE.toChatMessage(last));
    }
}
