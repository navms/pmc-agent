package io.github.navms.application.chat.service;

import io.github.navms.agent.agui.AguiAgentNames;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AguiAgentNamesTest {

    @Test
    void parentSourceMapsToGeneralChat() {
        assertEquals("general_chat", AguiAgentNames.fromSource(null));
        assertEquals("general_chat", AguiAgentNames.fromSource("pmc_supervisor"));
        assertEquals("query_bank", AguiAgentNames.fromSource("pmc_supervisor/query_bank"));
    }

    @Test
    void hidesParentHarnessTools() {
        assertTrue(AguiAgentNames.hideParentTool("general_chat", "agent_spawn"));
        assertFalse(AguiAgentNames.hideParentTool("query_bank", "queryAccount"));
    }
}
