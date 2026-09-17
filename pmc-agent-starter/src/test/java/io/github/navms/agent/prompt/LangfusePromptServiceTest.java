package io.github.navms.agent.prompt;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LangfusePromptServiceTest {

    @Test
    void renderReplacesMustacheVariables() {
        String out = LangfusePromptService.render("日期{{today}}，我是{{name}}", Map.of("today", "2026-09-17", "name", "何锦"));
        assertEquals("日期2026-09-17，我是何锦", out);
    }
}
