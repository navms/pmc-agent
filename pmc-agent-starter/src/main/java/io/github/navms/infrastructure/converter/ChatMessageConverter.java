package io.github.navms.infrastructure.converter;

import io.github.navms.domain.chat.entity.ChatMessage;
import io.github.navms.domain.chat.enums.ChatMessageType;
import io.github.navms.infrastructure.model.ChatMessageDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 消息 DO 与领域对象互转。
 *
 * @author navms
 */
@Mapper
public interface ChatMessageConverter {

    ChatMessageConverter INSTANCE = Mappers.getMapper(ChatMessageConverter.class);

    /**
     * @param source DO
     * @return 领域对象
     */
    default ChatMessage toChatMessage(ChatMessageDO source) {
        if (source == null) {
            return null;
        }
        return new ChatMessage.Builder(source.getSessionId(), ChatMessageType.fromCode(source.getRole()))
                .id(source.getId())
                .message(source.getMessage())
                .seq(source.getSeq())
                .createdAt(source.getCreatedAt())
                .deleted(source.getDeleted())
                .build();
    }

    /**
     * @param source 领域对象
     * @return DO
     */
    @Mapping(target = "role", source = "role.code")
    ChatMessageDO toChatMessageDO(ChatMessage source);
}
