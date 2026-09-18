package io.github.navms.application.chat.service;

import io.github.navms.agent.observability.LangfuseDatasetClient;
import io.github.navms.application.chat.dto.DislikeSessionCommand;
import io.github.navms.domain.chat.entity.ChatMessage;
import io.github.navms.domain.chat.entity.ChatSession;
import io.github.navms.domain.chat.enums.ChatMessageType;
import io.github.navms.domain.chat.exception.BusinessException;
import io.github.navms.domain.chat.exception.ChatErrorCode;
import io.github.navms.domain.chat.repository.ChatMessageRepository;
import io.github.navms.domain.chat.repository.ChatSessionRepository;
import io.github.navms.domain.chat.valueobj.SessionTitle;
import io.github.navms.domain.chat.valueobj.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionAppServiceDislikeTest {

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private LangfuseDatasetClient langfuseDatasetClient;

    private ChatSessionAppService service;

    @BeforeEach
    void setUp() {
        service = new ChatSessionAppService(sessionRepository, messageRepository, transactionTemplate, langfuseDatasetClient);
    }

    @Test
    void dislikeWritesSessionSnapshot() {
        ChatSession session = new ChatSession.Builder(UserId.of("user-1"))
                .id(9L)
                .title(new SessionTitle("查流水"))
                .build();
        when(sessionRepository.requireById(9L)).thenReturn(session);
        when(messageRepository.listBySessionId(9L)).thenReturn(List.of(
                message("m-user", "user", "hello"),
                message("m-assistant", "assistant", "world")));

        service.dislike(new DislikeSessionCommand(9L, "m-assistant", "down"));

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> inputCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> metadataCaptor = ArgumentCaptor.forClass(Object.class);
        verify(langfuseDatasetClient).upsertBaseCase(idCaptor.capture(), inputCaptor.capture(), metadataCaptor.capture());
        assertEquals("pmc-thumbsdown-9-m-assistant", idCaptor.getValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> input = (Map<String, Object>) inputCaptor.getValue();
        assertEquals(9L, input.get("sessionId"));
        assertEquals("查流水", input.get("title"));
        assertEquals(2, ((List<?>) input.get("messages")).size());

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) metadataCaptor.getValue();
        assertEquals("user-1", metadata.get("userId"));
        assertEquals("m-assistant", metadata.get("dislikedMessageId"));
        assertEquals("down", metadata.get("rating"));
    }

    @Test
    void dislikeRejectsMissingMessage() {
        ChatSession session = new ChatSession.Builder(UserId.of("user-1")).id(9L).build();
        when(sessionRepository.requireById(9L)).thenReturn(session);
        when(messageRepository.listBySessionId(9L)).thenReturn(List.of(message("other", "assistant", "x")));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.dislike(new DislikeSessionCommand(9L, "missing", "down")));
        assertEquals(ChatErrorCode.MESSAGE_NOT_FOUND, exception.getCode());
    }

    @Test
    void dislikeRejectsInvalidRating() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.dislike(new DislikeSessionCommand(9L, "m-1", "up")));
        assertEquals(ChatErrorCode.INVALID_FEEDBACK, exception.getCode());
    }

    private static ChatMessage message(String id, String role, String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id);
        payload.put("role", role);
        payload.put("content", content);
        return new ChatMessage.Builder(9L, ChatMessageType.fromCode(role)).message(payload).seq(0).build();
    }
}
