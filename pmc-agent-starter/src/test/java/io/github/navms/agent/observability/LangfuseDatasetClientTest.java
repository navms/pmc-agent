package io.github.navms.agent.observability;

import com.langfuse.client.LangfuseClient;
import com.langfuse.client.core.LangfuseClientApiException;
import com.langfuse.client.resources.datasetitems.DatasetItemsClient;
import com.langfuse.client.resources.datasetitems.types.CreateDatasetItemRequest;
import com.langfuse.client.resources.datasets.DatasetsClient;
import com.langfuse.client.resources.datasets.types.CreateDatasetRequest;
import io.github.navms.domain.chat.exception.BusinessException;
import io.github.navms.domain.chat.exception.ChatErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

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
    void upsertCreatesDatasetWhenMissing() {
        when(provider.getIfAvailable()).thenReturn(langfuseClient);
        when(langfuseClient.datasets()).thenReturn(datasetsClient);
        when(langfuseClient.datasetItems()).thenReturn(datasetItemsClient);
        when(datasetsClient.get("base-case")).thenThrow(new LangfuseClientApiException("missing", 404, null));

        client.upsertBaseCase("item-1", Map.of("sessionId", 1L), Map.of("rating", "down"));

        ArgumentCaptor<CreateDatasetRequest> datasetCaptor = ArgumentCaptor.forClass(CreateDatasetRequest.class);
        verify(datasetsClient).create(datasetCaptor.capture());
        assertEquals("base-case", datasetCaptor.getValue().getName());

        ArgumentCaptor<CreateDatasetItemRequest> itemCaptor = ArgumentCaptor.forClass(CreateDatasetItemRequest.class);
        verify(datasetItemsClient).create(itemCaptor.capture());
        assertEquals("base-case", itemCaptor.getValue().getDatasetName());
        assertEquals("item-1", itemCaptor.getValue().getId().orElseThrow());
    }

    @Test
    void upsertSkipsCreateWhenDatasetExists() {
        when(provider.getIfAvailable()).thenReturn(langfuseClient);
        when(langfuseClient.datasets()).thenReturn(datasetsClient);
        when(langfuseClient.datasetItems()).thenReturn(datasetItemsClient);

        client.upsertBaseCase("item-1", Map.of(), Map.of());

        verify(datasetsClient, never()).create(any());
        verify(datasetItemsClient).create(any(CreateDatasetItemRequest.class));
    }

    @Test
    void upsertFailsWhenLangfuseDisabled() {
        properties.setEnabled(false);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> client.upsertBaseCase("item-1", Map.of(), Map.of()));
        assertEquals(ChatErrorCode.LANGFUSE_UNAVAILABLE, exception.getCode());
    }
}
