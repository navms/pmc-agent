package io.github.navms.agent.eval;

import com.langfuse.client.resources.commons.types.DatasetItem;
import com.langfuse.client.resources.commons.types.DatasetStatus;
import io.github.navms.agent.observability.dataset.Input;
import io.github.navms.agent.observability.dataset.Metadata;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoldenTaskFromDatasetItemTest {

    @Test
    void mapsGoldenSchema() {
        DatasetItem item = DatasetItem.builder()
                .id("summarize-month-pay")
                .status(DatasetStatus.ACTIVE)
                .input(Map.of(
                        "title", "本月合计",
                        "turns", List.of(Map.of("role", "user", "content", "这个月支付合计多少"))))
                .expectedOutput(Map.of(
                        "taskType", "summarize",
                        "weight", "normal",
                        "layer", "edge",
                        "successCriteria", List.of(Map.of(
                                "id", "spawn_summarize",
                                "kind", "required_agent",
                                "value", "summarize_bank"))))
                .metadata(Map.of())
                .datasetId("ds")
                .datasetName("pmc-golden")
                .createdAt(OffsetDateTime.parse("2026-09-18T00:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-09-18T00:00:00Z"))
                .build();

        GoldenTask task = GoldenTask.fromDatasetItem(item);
        assertEquals("summarize-month-pay", task.id());
        assertEquals("本月合计", task.title());
        assertEquals("edge", task.layer());
        assertEquals(1, task.turns().size());
        assertEquals("这个月支付合计多少", task.turns().getFirst().content());
        assertTrue(task.hasCriteria());
        assertEquals("summarize_bank", task.expectedOutput().criteria().getFirst().stringValue());
        assertTrue(DatasetItems.hasSuccessCriteria(item));
    }

    @Test
    void thumbsDownWithoutExpectedIsNotCriteria() {
        DatasetItem item = DatasetItem.builder()
                .id("pmc-thumbsdown-1")
                .status(DatasetStatus.ACTIVE)
                .input(Map.of("sessionId", 1, "title", "查流水", "messages", List.of()))
                .expectedOutput(null)
                .metadata(Map.of("userId", "user-1", "dislikedMessageId", "m-1", "rating", "down"))
                .datasetId("ds")
                .datasetName("base-case")
                .createdAt(OffsetDateTime.parse("2026-09-18T00:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-09-18T00:00:00Z"))
                .build();

        GoldenTask task = GoldenTask.fromDatasetItem(item);
        assertEquals("查流水", task.title());
        assertTrue(task.turns().isEmpty());
        assertFalse(task.hasCriteria());
        assertFalse(DatasetItems.hasSuccessCriteria(item));
        Input input = DatasetItems.inputOf(item);
        assertEquals(1L, input.sessionId());
        Metadata metadata = DatasetItems.metadataOf(item);
        assertEquals("user-1", metadata.userId());
        assertEquals("m-1", metadata.dislikedMessageId());
        assertEquals("down", metadata.rating());
    }

    @Test
    void mapsBaseCaseUserMessagesAsTurns() {
        DatasetItem item = DatasetItem.builder()
                .id("pmc-thumbsdown-32")
                .status(DatasetStatus.ACTIVE)
                .input(Map.of(
                        "sessionId", 32,
                        "title", "6013821000000008008 在2029-01-23 的交易金额有多少",
                        "messages", List.of(
                                Map.of("id", "u1", "role", "user", "content", "交易金额有多少"),
                                Map.of("id", "a1", "role", "assistant", "content", "46 笔"),
                                Map.of("id", "u2", "role", "user", "content", "系统里面还有其他的账号吗"))))
                .expectedOutput(null)
                .metadata(Map.of("userId", "user-1", "dislikedMessageId", "u2", "rating", "down"))
                .datasetId("ds")
                .datasetName("base-case")
                .createdAt(OffsetDateTime.parse("2026-09-18T00:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-09-18T00:00:00Z"))
                .build();

        GoldenTask task = GoldenTask.fromDatasetItem(item);
        assertEquals(2, task.turns().size());
        assertEquals("系统里面还有其他的账号吗", task.turns().get(1).content());
        assertEquals("down", DatasetItems.metadataOf(item).rating());
        assertEquals(32L, DatasetItems.inputOf(item).sessionId());
    }
}
