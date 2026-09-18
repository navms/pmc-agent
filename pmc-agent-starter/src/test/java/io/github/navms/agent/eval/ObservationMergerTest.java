package io.github.navms.agent.eval;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationMergerTest {

    @Test
    void unionsAgentsAndKeepsLastInterrupt() {
        TranscriptObservation first = new TranscriptObservation(
                Set.of("summarize_bank"), Set.of(), false, List.of(), true, "合计", null, 1L);
        TranscriptObservation second = new TranscriptObservation(
                Set.of(), Set.of("submitBankPayOrder"), true, List.of("x"), false, "确认", null, 2L);
        TranscriptObservation merged = TranscriptObservation.merge(List.of(first, second), 30L);
        assertEquals(Set.of("summarize_bank"), merged.subAgents());
        assertEquals(Set.of("submitBankPayOrder"), merged.writeTools());
        assertTrue(merged.interrupted());
        assertEquals(List.of("x"), merged.interruptIds());
        assertTrue(merged.clarified());
        assertEquals(30L, merged.latencyMs());
    }
}
