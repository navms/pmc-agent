package io.github.navms.application.chat.service;

import io.github.navms.application.chat.converter.ChatAppConverter;
import io.github.navms.application.chat.dto.ChatMessageInfo;
import io.github.navms.application.chat.dto.ChatSessionInfo;
import io.github.navms.application.chat.dto.CreateSessionCommand;
import io.github.navms.application.chat.dto.RenameSessionCommand;
import io.github.navms.domain.chat.entity.ChatMessage;
import io.github.navms.domain.chat.entity.ChatSession;
import io.github.navms.domain.chat.enums.ChatMessageType;
import io.github.navms.domain.chat.repository.ChatMessageRepository;
import io.github.navms.domain.chat.repository.ChatSessionRepository;
import io.github.navms.domain.chat.valueobj.SessionTitle;
import io.github.navms.domain.chat.valueobj.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话用例编排。
 *
 * @author navms
 */
@Service
@RequiredArgsConstructor
public class ChatSessionAppService {

    private final ChatSessionRepository sessionRepository;

    private final ChatMessageRepository messageRepository;

    private final TransactionTemplate transactionTemplate;

    /**
     * @param command 创建命令
     * @return 新会话
     */
    public ChatSessionInfo create(CreateSessionCommand command) {
        return transactionTemplate.execute(status -> {
            ChatSession session = sessionRepository.insert(ChatSession.create(UserId.of(command.userId())));
            return ChatAppConverter.INSTANCE.toChatSessionInfo(session);
        });
    }

    /**
     * @param userId 用户标识
     * @return 会话列表
     */
    public List<ChatSessionInfo> listByUser(String userId) {
        return ChatAppConverter.INSTANCE.toChatSessionInfoList(sessionRepository.listByUserId(UserId.of(userId)));
    }

    /**
     * @param sessionId 会话 ID
     * @return 历史消息
     */
    public List<ChatMessageInfo> listMessages(Long sessionId) {
        sessionRepository.requireById(sessionId);
        return ChatAppConverter.INSTANCE.toChatMessageInfoList(messageRepository.listBySessionId(sessionId));
    }

    /**
     * @param command 重命名命令
     * @return 更新后的会话
     */
    public ChatSessionInfo rename(RenameSessionCommand command) {
        return transactionTemplate.execute(status -> {
            ChatSession session = sessionRepository.requireById(command.sessionId());
            session.rename(new SessionTitle(command.title()));
            sessionRepository.update(session);
            return ChatAppConverter.INSTANCE.toChatSessionInfo(session);
        });
    }

    /**
     * @param sessionId 会话 ID
     */
    public void softDelete(Long sessionId) {
        transactionTemplate.executeWithoutResult(status -> {
            sessionRepository.requireById(sessionId);
            sessionRepository.deleteById(sessionId);
        });
    }

    /**
     * 追加用户提问，空标题时用提问生成标题。
     *
     * @param sessionId 会话 ID
     * @param prompt    用户输入
     */
    public void appendUserPrompt(Long sessionId, String prompt) {
        transactionTemplate.executeWithoutResult(status -> {
            ChatSession session = sessionRepository.requireById(sessionId);
            session.applyFirstUserPrompt(prompt);
            sessionRepository.update(session);
            appendRaw(session, ChatMessageType.USER, prompt, Map.of("messageType", "user", "content", prompt));
        });
    }

    /**
     * 按消息类型与 payload 落库。
     *
     * @param sessionId   会话 ID
     * @param messageType 类型码
     * @param content     文本
     * @param payload     完整结构
     */
    public void appendMessage(Long sessionId, String messageType, String content, Map<String, Object> payload) {
        transactionTemplate.executeWithoutResult(status -> {
            ChatSession session = sessionRepository.requireById(sessionId);
            appendRaw(session, ChatMessageType.fromCode(messageType), content, payload);
        });
    }

    /**
     * 落一条助手完整文本。
     *
     * @param sessionId  会话 ID
     * @param text       文本
     * @param tokenUsage Token 用量，可空
     */
    public void appendAssistantText(Long sessionId, String text, Map<String, Object> tokenUsage) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            ChatSession session = sessionRepository.requireById(sessionId);
            Map<String, Object> payload = new HashMap<>();
            payload.put("messageType", "assistant");
            payload.put("content", text);
            if (tokenUsage != null && !tokenUsage.isEmpty()) {
                payload.put("tokenUsage", tokenUsage);
            }
            appendRaw(session, ChatMessageType.ASSISTANT, text, payload);
        });
    }

    /**
     * 更新会话状态。
     *
     * @param sessionId 会话 ID
     * @param status    状态码
     */
    public void markStatus(Long sessionId, String status) {
        transactionTemplate.executeWithoutResult(tx -> {
            ChatSession session = sessionRepository.requireById(sessionId);
            if ("interrupted".equals(status)) {
                session.markInterrupted();
            } else {
                session.markActive();
            }
            sessionRepository.update(session);
        });
    }

    /**
     * 确认会话存在。
     *
     * @param sessionId 会话 ID
     */
    public void requireSession(Long sessionId) {
        sessionRepository.requireById(sessionId);
    }

    private void appendRaw(ChatSession session, ChatMessageType type, String content, Map<String, Object> payload) {
        Integer lastSeq = messageRepository.findLastBySessionId(session.getId())
                .map(ChatMessage::getSeq)
                .orElse(null);
        ChatMessage message = ChatMessage.append(session.getId(), type, content, payload, lastSeq);
        messageRepository.insert(message);
        session.touch();
        sessionRepository.update(session);
    }
}
