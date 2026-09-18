package io.github.navms.agent.clarify;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentStartEvent;
import io.agentscope.core.event.CustomEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.middleware.AgentInput;
import io.github.navms.domain.chat.entity.ChatMessage;
import io.github.navms.domain.chat.enums.ChatMessageType;
import io.github.navms.domain.chat.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntentClarifyMiddlewareTest {

    @Mock
    private IntentClarifyService intentClarifyService;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private Agent agent;

    @Mock
    private RuntimeContext context;

    private IntentClarifyMiddleware middleware;

    @BeforeEach
    void setUp() {
        middleware = new IntentClarifyMiddleware(intentClarifyService, chatMessageRepository);
        when(agent.getName()).thenReturn("pmc_supervisor");
    }

    @Test
    void readyRewritesFollowUpWithInheritedDate() {
        when(context.getSessionId()).thenReturn("22");
        when(chatMessageRepository.listBySessionId(22L)).thenReturn(List.of(
                message(ChatMessageType.USER, "6013821000000008008 在2029-01-23 的交易金额有多少"),
                message(ChatMessageType.ASSISTANT, "合计 1,878,013 元")));
        when(intentClarifyService.classify(eq("那 6013821000000008000212 这个账号呢？"), anyList(), eq(context)))
                .thenReturn(Optional.of(new IntentClarifyResult(
                        "ready",
                        "summarize",
                        "汇总账号 6013821000000008000212 在 2029-01-23 的交易金额合计",
                        "",
                        List.of())));

        RunResult result = run(userInput("那 6013821000000008000212 这个账号呢？"));

        String text = lastUser(result.forwarded()).getTextContent();
        assertTrue(text.contains("【已澄清任务】"));
        assertTrue(text.contains("summarize"));
        assertTrue(text.contains("6013821000000008000212"));
        assertTrue(text.contains("2029-01-23"));
        assertClarifySequence(result.events());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<IntentClarifyParser.HistoryTurn>> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(intentClarifyService).classify(eq("那 6013821000000008000212 这个账号呢？"), historyCaptor.capture(), eq(context));
        List<IntentClarifyParser.HistoryTurn> history = historyCaptor.getValue();
        assertEquals(2, history.size());
        assertTrue(history.get(0).text().contains("6013821000000008008"));
        assertTrue(history.get(0).text().contains("2029-01-23"));
    }

    @Test
    void needClarifyRewritesToQuestionOnly() {
        when(context.getSessionId()).thenReturn("1");
        when(chatMessageRepository.listBySessionId(1L)).thenReturn(List.of());
        when(intentClarifyService.classify(eq("交易金额有多少"), anyList(), eq(context)))
                .thenReturn(Optional.of(new IntentClarifyResult(
                        "need_clarify", "summarize", "", "请问账号是多少？", List.of("accountNo"))));

        RunResult result = run(userInput("交易金额有多少"));
        String text = lastUser(result.forwarded()).getTextContent();
        assertTrue(text.contains("【意图澄清】"));
        assertTrue(text.contains("请问账号是多少？"));
        assertClarifySequence(result.events());
    }

    @Test
    void skipsHitlResume() {
        RunResult result = run(userInput("approved"));
        assertEquals("approved", lastUser(result.forwarded()).getTextContent());
        assertTrue(result.events().isEmpty());
        verify(intentClarifyService, never()).classify(any(), anyList(), any());
    }

    @Test
    void passesThroughWhenClassifyFails() {
        when(context.getSessionId()).thenReturn("1");
        when(chatMessageRepository.listBySessionId(1L)).thenReturn(List.of());
        when(intentClarifyService.classify(eq("你好"), anyList(), eq(context))).thenReturn(Optional.empty());

        RunResult result = run(userInput("你好"));
        assertEquals("你好", lastUser(result.forwarded()).getTextContent());
        assertClarifySequence(result.events());
    }

    @Test
    void skipsNonSupervisor() {
        when(agent.getName()).thenReturn("query_bank");
        RunResult result = run(userInput("查流水"));
        assertEquals("查流水", lastUser(result.forwarded()).getTextContent());
        assertTrue(result.events().isEmpty());
        verify(intentClarifyService, never()).classify(any(), anyList(), any());
    }

    private RunResult run(AgentInput input) {
        AtomicReference<AgentInput> captured = new AtomicReference<>();
        List<AgentEvent> events = middleware.onAgent(agent, context, input, next -> {
            captured.set(next);
            return Flux.empty();
        }).collectList().block(Duration.ofSeconds(5));
        return new RunResult(captured.get(), events == null ? List.of() : events);
    }

    @Test
    void dropsDuplicateAgentStartFromNext() {
        when(context.getSessionId()).thenReturn("1");
        when(chatMessageRepository.listBySessionId(1L)).thenReturn(List.of());
        when(intentClarifyService.classify(eq("查流水"), anyList(), eq(context))).thenReturn(Optional.empty());

        AtomicReference<AgentInput> captured = new AtomicReference<>();
        List<AgentEvent> events = middleware.onAgent(agent, context, userInput("查流水"), next -> {
            captured.set(next);
            return Flux.just(new AgentStartEvent(null, "core-reply", "pmc_supervisor"));
        }).collectList().block(Duration.ofSeconds(5));

        assertEquals("查流水", lastUser(captured.get()).getTextContent());
        assertClarifySequence(events == null ? List.of() : events);
    }

    private static void assertClarifySequence(List<AgentEvent> events) {
        assertEquals(3, events.size());
        assertInstanceOf(AgentStartEvent.class, events.get(0));
        CustomEvent start = assertInstanceOf(CustomEvent.class, events.get(1));
        CustomEvent end = assertInstanceOf(CustomEvent.class, events.get(2));
        assertEquals(IntentClarifyMiddleware.EVENT_START, start.getName());
        assertEquals(IntentClarifyMiddleware.EVENT_END, end.getName());
    }

    private static AgentInput userInput(String text) {
        return new AgentInput(List.of(Msg.builder().role(MsgRole.USER).name("user").textContent(text).build()));
    }

    private static Msg lastUser(AgentInput input) {
        return IntentClarifyMiddleware.lastUserMessage(input.msgs());
    }

    private static ChatMessage message(ChatMessageType role, String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", "m-" + role.getCode());
        payload.put("role", role.getCode());
        payload.put("content", content);
        return new ChatMessage.Builder(22L, role).message(payload).seq(0).build();
    }

    private record RunResult(AgentInput forwarded, List<AgentEvent> events) {
        RunResult {
            events = events == null ? List.of() : List.copyOf(new ArrayList<>(events));
        }
    }
}
