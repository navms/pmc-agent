package io.github.navms.agent.clarify;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.MessageMetadataKeys;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntentClarifyServiceTest {

    @Mock
    private ReActAgent intentClarifyAgent;

    @Mock
    private RuntimeContext context;

    private IntentClarifyService service;

    @BeforeEach
    void setUp() {
        service = new IntentClarifyService(intentClarifyAgent);
    }

    @Test
    void classifyReadsStructuredOutput() {
        IntentClarifyResult payload = new IntentClarifyResult(
                "ready",
                "summarize",
                "汇总账号 6013821000000008000212 在 2029-01-23 的交易金额合计",
                "",
                List.of());
        when(intentClarifyAgent.call(anyList(), eq(IntentClarifyResult.class), eq(context)))
                .thenReturn(Mono.just(structuredMsg(payload)));

        Optional<IntentClarifyResult> result = service.classify(
                "那 6013821000000008000212 这个账号呢？",
                List.of(new IntentClarifyParser.HistoryTurn(
                        "user", "6013821000000008008 在2029-01-23 的交易金额有多少")),
                context);

        assertTrue(result.isPresent());
        assertTrue(result.get().ready());
        assertEquals("summarize", result.get().intent());
        assertTrue(result.get().enhancedPrompt().contains("6013821000000008000212"));
        assertTrue(result.get().enhancedPrompt().contains("2029-01-23"));
    }

    @Test
    void classifyFailsOpenWhenCallErrors() {
        when(intentClarifyAgent.call(anyList(), eq(IntentClarifyResult.class), any()))
                .thenReturn(Mono.error(new IllegalStateException("boom")));

        Optional<IntentClarifyResult> result = service.classify("你好", List.of(), context);
        assertTrue(result.isEmpty());
    }

    @Test
    void classifyFailsOpenWhenStatusInvalid() {
        IntentClarifyResult payload = new IntentClarifyResult("maybe", "query", "", "", List.of());
        when(intentClarifyAgent.call(anyList(), eq(IntentClarifyResult.class), eq(context)))
                .thenReturn(Mono.just(structuredMsg(payload)));

        Optional<IntentClarifyResult> result = service.classify("查一下", List.of(), context);
        assertTrue(result.isEmpty());
    }

    @Test
    void classifySkipsBlank() {
        assertTrue(service.classify("  ", List.of(), context).isEmpty());
    }

    private static Msg structuredMsg(IntentClarifyResult payload) {
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("status", payload.status());
        structured.put("intent", payload.intent());
        structured.put("enhancedPrompt", payload.enhancedPrompt());
        structured.put("question", payload.question());
        structured.put("missingSlots", payload.missingSlots());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(MessageMetadataKeys.STRUCTURED_OUTPUT, structured);
        return Msg.builder().role(MsgRole.ASSISTANT).name("intent_clarify").textContent("").metadata(metadata).build();
    }
}
