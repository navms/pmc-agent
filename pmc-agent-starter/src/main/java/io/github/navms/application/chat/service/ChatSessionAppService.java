package io.github.navms.application.chat.service;

import io.github.navms.agent.observability.LangfuseDatasetClient;
import io.github.navms.agent.observability.dataset.Input;
import io.github.navms.agent.observability.dataset.Metadata;
import io.github.navms.application.chat.converter.ChatAppConverter;
import io.github.navms.application.chat.dto.ChatMessageInfo;
import io.github.navms.application.chat.dto.ChatSessionInfo;
import io.github.navms.application.chat.dto.CreateSessionCommand;
import io.github.navms.application.chat.dto.DislikeSessionCommand;
import io.github.navms.application.chat.dto.RenameSessionCommand;
import io.github.navms.domain.chat.entity.ChatMessage;
import io.github.navms.domain.chat.entity.ChatSession;
import io.github.navms.domain.chat.exception.BusinessException;
import io.github.navms.domain.chat.exception.ChatErrorCode;
import io.github.navms.domain.chat.repository.ChatMessageRepository;
import io.github.navms.domain.chat.repository.ChatSessionRepository;
import io.github.navms.domain.chat.valueobj.SessionTitle;
import io.github.navms.domain.chat.valueobj.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private final LangfuseDatasetClient langfuseDatasetClient;

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
     * 将本次会话写入 Langfuse Dataset（base case）。
     *
     * @param command 点踩命令
     */
    public void dislike(DislikeSessionCommand command) {
        if (command == null || !StringUtils.hasText(command.messageId()) || !"down".equalsIgnoreCase(command.rating())) {
            throw new BusinessException(ChatErrorCode.INVALID_FEEDBACK);
        }
        ChatSession session = sessionRepository.requireById(command.sessionId());
        List<ChatMessage> rows = messageRepository.listBySessionId(session.getId());
        boolean messageExists = rows.stream().anyMatch(row -> command.messageId().equals(String.valueOf(row.getMessage().get("id"))));
        if (!messageExists) {
            throw new BusinessException(ChatErrorCode.MESSAGE_NOT_FOUND);
        }
        Input input = Input.from(session, rows);
        Metadata metadata = Metadata.dislike(session.getUserId().value(), command.messageId());
        String itemId = "pmc-thumbsdown-" + session.getId() + "-" + command.messageId();
        langfuseDatasetClient.upsertBaseCase(itemId, input, metadata);
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
            appendRaw(session, userMessage(prompt));
        });
    }

    /**
     * 落库一条 AG-UI Message。
     *
     * @param sessionId 会话 ID
     * @param message   协议对象
     */
    public void appendMessage(Long sessionId, Map<String, Object> message) {
        transactionTemplate.executeWithoutResult(status -> {
            ChatSession session = sessionRepository.requireById(sessionId);
            appendRaw(session, message);
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

    private void appendRaw(ChatSession session, Map<String, Object> message) {
        Integer lastSeq = messageRepository.findLastBySessionId(session.getId())
                .map(ChatMessage::getSeq)
                .orElse(null);
        ChatMessage row = ChatMessage.append(session.getId(), message, lastSeq);
        messageRepository.insert(row);
        session.touch();
        sessionRepository.update(session);
    }

    private static Map<String, Object> userMessage(String prompt) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("id", UUID.randomUUID().toString());
        message.put("role", "user");
        message.put("content", prompt);
        return message;
    }
}
