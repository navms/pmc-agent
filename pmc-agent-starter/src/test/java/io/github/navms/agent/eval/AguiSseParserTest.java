package io.github.navms.agent.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AguiSseParserTest {

    private final AguiSseParser parser = new AguiSseParser(new ObjectMapper());

    @Test
    void extractsSpawnFromCustomLifecycle() throws Exception {
        String sse = """
                event: CUSTOM
                data: {"type":"CUSTOM","name":"intent_clarify.start","value":{}}

                event: CUSTOM
                data: {"type":"CUSTOM","name":"subagent.lifecycle","value":{"type":"AGENT_START","source":"query_bank"}}

                event: RUN_FINISHED
                data: {"type":"RUN_FINISHED","outcome":"success"}

                """;
        TranscriptObservation observation = parser.parse(new StringReader(sse));
        assertEquals(Set.of("query_bank"), observation.subAgents());
        assertTrue(observation.clarified());
        assertEquals(false, observation.interrupted());
    }

    @Test
    void extractsSpawnArgsAndWriteInterrupt() throws Exception {
        String sse = """
                event: TOOL_CALL_START
                data: {"type":"TOOL_CALL_START","toolCallId":"1","toolCallName":"agent_spawn"}

                event: TOOL_CALL_ARGS
                data: {"type":"TOOL_CALL_ARGS","toolCallId":"1","delta":"{\\"name\\":\\"summarize_bank\\"}"}

                event: TOOL_CALL_END
                data: {"type":"TOOL_CALL_END","toolCallId":"1"}

                event: TOOL_CALL_START
                data: {"type":"TOOL_CALL_START","toolCallId":"2","toolCallName":"submitBankPayOrder"}

                event: RUN_FINISHED
                data: {"type":"RUN_FINISHED","outcome":"interrupt","interrupts":[{"id":"2"}]}

                """;
        TranscriptObservation observation = parser.parse(new StringReader(sse));
        assertEquals(Set.of("summarize_bank"), observation.subAgents());
        assertEquals(Set.of("submitBankPayOrder"), observation.writeTools());
        assertTrue(observation.interrupted());
        assertEquals("2", observation.interruptIds().getFirst());
    }
}
