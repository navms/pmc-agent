package io.github.navms.agent.middle;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangfuseSessionMiddlewareTest {

    @Test
    void serializeModelOutputKeepsPlainTextWhenNoReasoning() {
        assertEquals("hello", LangfuseSessionMiddleware.serializeModelOutput("hello", "", List.of()));
    }

    @Test
    void serializeModelOutputIncludesReasoning() {
        String json = LangfuseSessionMiddleware.serializeModelOutput(
                "最终回答", "先想一步", List.of(Map.of("id", "1", "name", "query_bank")));
        assertTrue(json.contains("\"reasoning\":\"先想一步\""));
        assertTrue(json.contains("\"text\":\"最终回答\""));
        assertTrue(json.contains("query_bank"));
    }
}
