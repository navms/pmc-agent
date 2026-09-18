package io.github.navms.web.agent;

import io.github.navms.application.chat.dto.DislikeSessionCommand;
import io.github.navms.application.chat.service.ChatSessionAppService;
import io.github.navms.web.agent.converter.ChatWebConverter;
import io.github.navms.web.agent.param.CreateSessionRequest;
import io.github.navms.web.agent.param.RenameSessionRequest;
import io.github.navms.web.agent.param.SessionFeedbackRequest;
import io.github.navms.web.agent.vo.ChatMessageVO;
import io.github.navms.web.agent.vo.ChatSessionVO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会话列表与历史。
 *
 * @author navms
 */
@RestController
@RequestMapping("/sessions")
public class ChatSessionController {

    private final ChatSessionAppService chatSessionAppService;

    public ChatSessionController(ChatSessionAppService chatSessionAppService) {
        this.chatSessionAppService = chatSessionAppService;
    }

    /**
     * @param userId 用户标识
     * @return 会话列表
     */
    @GetMapping
    public List<ChatSessionVO> list(@RequestParam String userId) {
        return ChatWebConverter.INSTANCE.toChatSessionVOList(chatSessionAppService.listByUser(userId));
    }

    /**
     * @param request 创建请求
     * @return 新会话
     */
    @PostMapping
    public ChatSessionVO create(@RequestBody CreateSessionRequest request) {
        return ChatWebConverter.INSTANCE.toChatSessionVO(
                chatSessionAppService.create(ChatWebConverter.INSTANCE.toCreateSessionCommand(request)));
    }

    /**
     * @param sessionId 会话 ID
     * @return 历史消息
     */
    @GetMapping("/{sessionId}/messages")
    public List<ChatMessageVO> messages(@PathVariable Long sessionId) {
        return ChatWebConverter.INSTANCE.toChatMessageVOList(chatSessionAppService.listMessages(sessionId));
    }

    /**
     * @param sessionId 会话 ID
     * @param request   重命名请求
     * @return 更新后的会话
     */
    @PatchMapping("/{sessionId}")
    public ChatSessionVO rename(@PathVariable Long sessionId, @RequestBody RenameSessionRequest request) {
        return ChatWebConverter.INSTANCE.toChatSessionVO(
                chatSessionAppService.rename(ChatWebConverter.INSTANCE.toRenameSessionCommand(sessionId, request)));
    }

    /**
     * @param sessionId 会话 ID
     */
    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long sessionId) {
        chatSessionAppService.softDelete(sessionId);
    }

    /**
     * 点踩：将本次会话写入 Langfuse Dataset。
     *
     * @param sessionId 会话 ID
     * @param request   被踩消息
     */
    @PostMapping("/{sessionId}/feedback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void feedback(@PathVariable Long sessionId, @RequestBody SessionFeedbackRequest request) {
        chatSessionAppService.dislike(new DislikeSessionCommand(
                sessionId,
                request == null ? null : request.messageId(),
                request == null ? null : request.rating()));
    }
}
