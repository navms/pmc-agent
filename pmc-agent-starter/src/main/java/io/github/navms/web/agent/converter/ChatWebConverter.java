package io.github.navms.web.agent.converter;

import io.github.navms.application.chat.dto.ChatMessageInfo;
import io.github.navms.application.chat.dto.ChatSessionInfo;
import io.github.navms.application.chat.dto.CreateSessionCommand;
import io.github.navms.application.chat.dto.RenameSessionCommand;
import io.github.navms.application.chat.dto.ResumeChatCommand;
import io.github.navms.application.chat.dto.StreamChatCommand;
import io.github.navms.web.agent.param.ChatRequest;
import io.github.navms.web.agent.param.ChatResumeRequest;
import io.github.navms.web.agent.param.CreateSessionRequest;
import io.github.navms.web.agent.param.RenameSessionRequest;
import io.github.navms.web.agent.vo.ChatMessageVO;
import io.github.navms.web.agent.vo.ChatSessionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Web Request/VO 与应用层 DTO 互转。
 *
 * @author navms
 */
@Mapper
public interface ChatWebConverter {

    ChatWebConverter INSTANCE = Mappers.getMapper(ChatWebConverter.class);

    /**
     * @param request 创建请求
     * @return 命令
     */
    CreateSessionCommand toCreateSessionCommand(CreateSessionRequest request);

    /**
     * @param sessionId 会话 ID
     * @param request   重命名请求
     * @return 命令
     */
    @Mapping(target = "sessionId", source = "sessionId")
    @Mapping(target = "title", expression = "java(request == null ? null : request.title())")
    RenameSessionCommand toRenameSessionCommand(Long sessionId, RenameSessionRequest request);

    /**
     * @param request 流式请求
     * @return 命令
     */
    StreamChatCommand toStreamChatCommand(ChatRequest request);

    /**
     * @param request 恢复请求
     * @return 命令
     */
    ResumeChatCommand toResumeChatCommand(ChatResumeRequest request);

    /**
     * @param source Info
     * @return VO
     */
    ChatSessionVO toChatSessionVO(ChatSessionInfo source);

    /**
     * @param source Info 列表
     * @return VO 列表
     */
    List<ChatSessionVO> toChatSessionVOList(List<ChatSessionInfo> source);

    /**
     * @param source Info
     * @return VO
     */
    ChatMessageVO toChatMessageVO(ChatMessageInfo source);

    /**
     * @param source Info 列表
     * @return VO 列表
     */
    List<ChatMessageVO> toChatMessageVOList(List<ChatMessageInfo> source);
}
