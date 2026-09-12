package io.github.navms.application.chat.converter;

import io.github.navms.application.chat.dto.ChatMessageInfo;
import io.github.navms.application.chat.dto.ChatSessionInfo;
import io.github.navms.domain.chat.entity.ChatMessage;
import io.github.navms.domain.chat.entity.ChatSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 领域对象到应用 Info。
 *
 * @author navms
 */
@Mapper
public interface ChatAppConverter {

    ChatAppConverter INSTANCE = Mappers.getMapper(ChatAppConverter.class);

    /**
     * @param source 会话
     * @return Info
     */
    @Mapping(target = "title", source = "title.value")
    @Mapping(target = "status", source = "status.code")
    ChatSessionInfo toChatSessionInfo(ChatSession source);

    /**
     * @param source 会话列表
     * @return Info 列表
     */
    List<ChatSessionInfo> toChatSessionInfoList(List<ChatSession> source);

    /**
     * @param source 消息
     * @return Info
     */
    default ChatMessageInfo toChatMessageInfo(ChatMessage source) {
        if (source == null) {
            return null;
        }
        return new ChatMessageInfo(source.toProtocolMessage());
    }

    /**
     * @param source 消息列表
     * @return Info 列表
     */
    List<ChatMessageInfo> toChatMessageInfoList(List<ChatMessage> source);
}
