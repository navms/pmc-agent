package io.github.navms.infrastructure.chat.converter;

import io.github.navms.domain.chat.entity.ChatMessage;
import io.github.navms.domain.chat.enums.ChatMessageType;
import io.github.navms.infrastructure.chat.model.ChatMessageDO;
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
     * 实体使用手写 Builder，DO → Domain 在此组装。
     *
     * @param source DO
     * @return 领域对象
     */
    default ChatMessage toChatMessage(ChatMessageDO source) {
        if (source == null) {
            return null;
        }
        return new ChatMessage.Builder(source.getSessionId(), ChatMessageType.fromCode(source.getMessageType()))
                .id(source.getId())
                .content(source.getContent())
                .payload(source.getPayload())
                .seq(source.getSeq())
                .createdAt(source.getCreatedAt())
                .deleted(source.getDeleted())
                .build();
    }

    /**
     * @param source 领域对象
     * @return DO
     */
    @Mapping(target = "messageType", source = "messageType.code")
    ChatMessageDO toChatMessageDO(ChatMessage source);
}
