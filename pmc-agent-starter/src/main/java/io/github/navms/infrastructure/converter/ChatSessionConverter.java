package io.github.navms.infrastructure.converter;

import io.github.navms.domain.chat.entity.ChatSession;
import io.github.navms.domain.chat.enums.ChatSessionStatus;
import io.github.navms.domain.chat.valueobj.SessionTitle;
import io.github.navms.domain.chat.valueobj.UserId;
import io.github.navms.infrastructure.model.ChatSessionDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 会话 DO 与领域对象互转。
 *
 * @author navms
 */
@Mapper
public interface ChatSessionConverter {

    ChatSessionConverter INSTANCE = Mappers.getMapper(ChatSessionConverter.class);

    /**
     * 实体使用手写 Builder，DO → Domain 在此组装。
     *
     * @param source DO
     * @return 领域对象
     */
    default ChatSession toChatSession(ChatSessionDO source) {
        if (source == null) {
            return null;
        }
        return new ChatSession.Builder(UserId.of(source.getUserId()))
                .id(source.getId())
                .title(source.getTitle() == null ? null : new SessionTitle(source.getTitle()))
                .status(ChatSessionStatus.fromCode(source.getStatus()))
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .deleted(source.getDeleted())
                .build();
    }

    /**
     * @param source 领域对象
     * @return DO
     */
    @Mapping(target = "userId", source = "userId.value")
    @Mapping(target = "title", source = "title.value")
    @Mapping(target = "status", source = "status.code")
    ChatSessionDO toChatSessionDO(ChatSession source);
}
