package io.github.navms.agent.observability;

import com.langfuse.client.LangfuseClient;
import com.langfuse.client.core.LangfuseClientApiException;
import com.langfuse.client.resources.commons.types.DatasetItem;
import com.langfuse.client.resources.commons.types.DatasetStatus;
import com.langfuse.client.resources.datasetitems.DatasetItemsClient;
import com.langfuse.client.resources.datasetitems.requests.GetDatasetItemsRequest;
import com.langfuse.client.resources.datasetitems.types.CreateDatasetItemRequest;
import com.langfuse.client.resources.score.ScoreClient;
import com.langfuse.client.resources.score.types.CreateScoreRequest;
import com.langfuse.client.resources.datasetitems.types.PaginatedDatasetItems;
import com.langfuse.client.resources.datasets.DatasetsClient;
import com.langfuse.client.resources.datasets.types.CreateDatasetRequest;
import com.langfuse.client.resources.utils.pagination.types.MetaResponse;
import io.github.navms.agent.observability.dataset.Input;
import io.github.navms.agent.observability.dataset.Metadata;
import io.github.navms.domain.chat.exception.BusinessException;
import io.github.navms.domain.chat.exception.ChatErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LangfuseDatasetClientTest {

    @Mock
    private ObjectProvider<LangfuseClient> provider;

    @Mock
    private LangfuseClient langfuseClient;

    @Mock
    private DatasetsClient datasetsClient;

    @Mock
    private DatasetItemsClient datasetItemsClient;

    @Mock
    private ScoreClient scoreClient;

    private LangfuseProperties properties;

    private LangfuseDatasetClient client;

    @BeforeEach
    void setUp() {
        properties = new LangfuseProperties();
        properties.setEnabled(true);
        properties.setHost("http://127.0.0.1:3000");
        properties.setPublicKey("pk");
        properties.setSecretKey("sk");
        properties.setBaseCaseDataset("base-case");
        client = new LangfuseDatasetClient(properties, provider);
    }

    @Test
    void listItemsUsesSdk() {
        when(provider.getIfAvailable()).thenReturn(langfuseClient);
        when(langfuseClient.datasetItems()).thenReturn(datasetItemsClient);
        DatasetItem item = DatasetItem.builder()
                .id("item-1")
                .status(DatasetStatus.ACTIVE)
                .input(Map.of("turns", List.of()))
                .expectedOutput(Map.of())
                .metadata(Map.of())
                .datasetId("ds")
                .datasetName("base-case")
                .createdAt(OffsetDateTime.parse("2026-09-18T00:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-09-18T00:00:00Z"))
                .build();
        MetaResponse meta = MetaResponse.builder().page(1).limit(50).totalItems(1).totalPages(1).build();
        PaginatedDatasetItems page = PaginatedDatasetItems.builder().meta(meta).data(List.of(item)).build();
        when(datasetItemsClient.list(any(GetDatasetItemsRequest.class))).thenReturn(page);

        List<DatasetItem> items = client.listItems("base-case");
        assertEquals(1, items.size());
        assertEquals("item-1", items.getFirst().getId());
    }

    @Test
    void createScoreUsesSessionId() {
        when(provider.getIfAvailable()).thenReturn(langfuseClient);
        when(langfuseClient.score()).thenReturn(scoreClient);

        client.createScore("32", "task_complete", 1.0, "全部成功标准通过", Map.of("category", ""));

        ArgumentCaptor<CreateScoreRequest> captor = ArgumentCaptor.forClass(CreateScoreRequest.class);
        verify(scoreClient).create(captor.capture());
        assertEquals("task_complete", captor.getValue().getName());
        assertEquals("32", captor.getValue().getSessionId().orElseThrow());
    }

    @Test
    void upsertCreatesDatasetWhenMissing() {
        when(provider.getIfAvailable()).thenReturn(langfuseClient);
        when(langfuseClient.datasets()).thenReturn(datasetsClient);
        when(langfuseClient.datasetItems()).thenReturn(datasetItemsClient);
        when(datasetsClient.get("base-case")).thenThrow(new LangfuseClientApiException("missing", 404, null));

        client.upsertBaseCase("item-1", new Input(null, 1L, List.of()), Metadata.dislike(null, null));

        ArgumentCaptor<CreateDatasetRequest> datasetCaptor = ArgumentCaptor.forClass(CreateDatasetRequest.class);
        verify(datasetsClient).create(datasetCaptor.capture());
        assertEquals("base-case", datasetCaptor.getValue().getName());

        ArgumentCaptor<CreateDatasetItemRequest> itemCaptor = ArgumentCaptor.forClass(CreateDatasetItemRequest.class);
        verify(datasetItemsClient).create(itemCaptor.capture());
        assertEquals("base-case", itemCaptor.getValue().getDatasetName());
        assertEquals("item-1", itemCaptor.getValue().getId().orElseThrow());
    }

    @Test
    void upsertGoldenWritesExpectedOutput() {
        when(provider.getIfAvailable()).thenReturn(langfuseClient);
        when(langfuseClient.datasets()).thenReturn(datasetsClient);
        when(langfuseClient.datasetItems()).thenReturn(datasetItemsClient);

        Map<String, Object> expected = Map.of("taskType", "summarize", "successCriteria", java.util.List.of());
        client.upsertGoldenItem("pmc-golden", "sum-1", Map.of("turns", java.util.List.of()), expected, Map.of("seed", true));

        ArgumentCaptor<CreateDatasetItemRequest> itemCaptor = ArgumentCaptor.forClass(CreateDatasetItemRequest.class);
        verify(datasetItemsClient).create(itemCaptor.capture());
        assertEquals("pmc-golden", itemCaptor.getValue().getDatasetName());
        assertEquals("sum-1", itemCaptor.getValue().getId().orElseThrow());
        assertEquals(expected, itemCaptor.getValue().getExpectedOutput().orElseThrow());
    }

    @Test
    void upsertSkipsCreateWhenDatasetExists() {
        when(provider.getIfAvailable()).thenReturn(langfuseClient);
        when(langfuseClient.datasets()).thenReturn(datasetsClient);
        when(langfuseClient.datasetItems()).thenReturn(datasetItemsClient);

        client.upsertBaseCase("item-1", new Input(null, null, List.of()), Metadata.dislike(null, null));

        verify(datasetsClient, never()).create(any());
        verify(datasetItemsClient).create(any(CreateDatasetItemRequest.class));
    }

    @Test
    void upsertFailsWhenLangfuseDisabled() {
        properties.setEnabled(false);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> client.upsertBaseCase("item-1", new Input(null, null, List.of()), Metadata.dislike(null, null)));
        assertEquals(ChatErrorCode.LANGFUSE_UNAVAILABLE, exception.getCode());
    }
}
