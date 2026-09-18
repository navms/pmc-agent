package io.github.navms.agent.observability;

import com.langfuse.client.LangfuseClient;
import com.langfuse.client.core.LangfuseClientApiException;
import com.langfuse.client.resources.datasetitems.types.CreateDatasetItemRequest;
import com.langfuse.client.resources.datasets.types.CreateDatasetRequest;
import io.github.navms.domain.chat.exception.BusinessException;
import io.github.navms.domain.chat.exception.ChatErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Langfuse Dataset / Dataset Item（base case）Public API。
 *
 * @author navms
 */
@Slf4j
@Component
public class LangfuseDatasetClient {

    private final LangfuseProperties properties;

    private final ObjectProvider<LangfuseClient> langfuseClient;

    public LangfuseDatasetClient(LangfuseProperties properties, ObjectProvider<LangfuseClient> langfuseClient) {
        this.properties = properties;
        this.langfuseClient = langfuseClient;
    }

    /**
     * 确保 Dataset 存在后 upsert 一条 item。
     *
     * @param itemId   稳定主键，用于幂等 upsert
     * @param input    Dataset item input
     * @param metadata Dataset item metadata
     */
    public void upsertBaseCase(String itemId, Object input, Object metadata) {
        LangfuseClient client = requireClient();
        String datasetName = properties.getBaseCaseDataset();
        if (!StringUtils.hasText(datasetName)) {
            throw new BusinessException(ChatErrorCode.LANGFUSE_UNAVAILABLE, "Langfuse base-case dataset is not configured");
        }
        ensureDataset(client, datasetName);
        try {
            client.datasetItems().create(CreateDatasetItemRequest.builder()
                    .datasetName(datasetName)
                    .id(itemId)
                    .input(input)
                    .metadata(metadata)
                    .build());
        } catch (LangfuseClientApiException e) {
            log.warn("Langfuse upsert dataset item {} failed: {} {}", itemId, e.statusCode(), e.getMessage());
            throw new BusinessException(ChatErrorCode.LANGFUSE_UNAVAILABLE, "failed to record Langfuse base case");
        } catch (Exception e) {
            log.warn("Langfuse upsert dataset item {} failed: {}", itemId, e.getMessage());
            throw new BusinessException(ChatErrorCode.LANGFUSE_UNAVAILABLE, "failed to record Langfuse base case");
        }
    }

    private void ensureDataset(LangfuseClient client, String datasetName) {
        try {
            client.datasets().get(datasetName);
            return;
        } catch (LangfuseClientApiException e) {
            if (e.statusCode() != 404) {
                log.warn("Langfuse GET dataset {} failed: {} {}", datasetName, e.statusCode(), e.getMessage());
                throw new BusinessException(ChatErrorCode.LANGFUSE_UNAVAILABLE, "failed to read Langfuse dataset");
            }
        } catch (Exception e) {
            log.warn("Langfuse GET dataset {} failed: {}", datasetName, e.getMessage());
            throw new BusinessException(ChatErrorCode.LANGFUSE_UNAVAILABLE, "failed to read Langfuse dataset");
        }
        try {
            client.datasets().create(CreateDatasetRequest.builder()
                    .name(datasetName)
                    .description("PMC agent thumbs-down base cases")
                    .build());
        } catch (LangfuseClientApiException e) {
            if (e.statusCode() == 409) {
                return;
            }
            log.warn("Langfuse CREATE dataset {} failed: {} {}", datasetName, e.statusCode(), e.getMessage());
            throw new BusinessException(ChatErrorCode.LANGFUSE_UNAVAILABLE, "failed to create Langfuse dataset");
        } catch (Exception e) {
            log.warn("Langfuse CREATE dataset {} failed: {}", datasetName, e.getMessage());
            throw new BusinessException(ChatErrorCode.LANGFUSE_UNAVAILABLE, "failed to create Langfuse dataset");
        }
    }

    private LangfuseClient requireClient() {
        if (!properties.isExportEnabled()) {
            throw new BusinessException(ChatErrorCode.LANGFUSE_UNAVAILABLE, "Langfuse is not enabled");
        }
        LangfuseClient client = langfuseClient.getIfAvailable();
        if (client == null) {
            throw new BusinessException(ChatErrorCode.LANGFUSE_UNAVAILABLE, "Langfuse is not enabled");
        }
        return client;
    }
}
