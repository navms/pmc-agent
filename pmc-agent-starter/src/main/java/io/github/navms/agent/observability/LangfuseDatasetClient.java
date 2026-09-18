package io.github.navms.agent.observability;

import com.langfuse.client.LangfuseClient;
import com.langfuse.client.core.LangfuseClientApiException;
import com.langfuse.client.resources.commons.types.CreateScoreValue;
import com.langfuse.client.resources.commons.types.DatasetItem;
import com.langfuse.client.resources.commons.types.ScoreDataType;
import com.langfuse.client.resources.datasetitems.requests.GetDatasetItemsRequest;
import com.langfuse.client.resources.datasetitems.types.CreateDatasetItemRequest;
import com.langfuse.client.resources.datasetitems.types.PaginatedDatasetItems;
import com.langfuse.client.resources.datasets.types.CreateDatasetRequest;
import com.langfuse.client.resources.score.types.CreateScoreRequest;
import io.github.navms.agent.observability.dataset.Input;
import io.github.navms.agent.observability.dataset.Metadata;
import io.github.navms.domain.chat.exception.BusinessException;
import io.github.navms.domain.chat.exception.ChatErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Langfuse Dataset / Score，全部经官方 {@link LangfuseClient}。
 *
 * @author navms
 */
@Slf4j
@Component
public class LangfuseDatasetClient {

    private final LangfuseProperties properties;

    private final ObjectProvider<LangfuseClient> langfuseClient;

    public LangfuseDatasetClient(
            LangfuseProperties properties,
            ObjectProvider<LangfuseClient> langfuseClient) {
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
    public void upsertBaseCase(String itemId, Input input, Metadata metadata) {
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

    /**
     * upsert Golden item（含 expectedOutput）。
     *
     * @param datasetName    Dataset
     * @param itemId         稳定 id
     * @param input          input
     * @param expectedOutput 成功标准
     * @param metadata       元数据
     */
    public void upsertGoldenItem(String datasetName, String itemId, Object input, Object expectedOutput, Object metadata) {
        LangfuseClient client = requireClient();
        if (!StringUtils.hasText(datasetName)) {
            throw new BusinessException(ChatErrorCode.LANGFUSE_UNAVAILABLE, "Langfuse golden dataset is not configured");
        }
        ensureDataset(client, datasetName);
        try {
            client.datasetItems().create(CreateDatasetItemRequest.builder()
                    .datasetName(datasetName)
                    .id(itemId)
                    .input(input)
                    .expectedOutput(expectedOutput)
                    .metadata(metadata)
                    .build());
        } catch (LangfuseClientApiException e) {
            log.warn("Langfuse upsert golden item {} failed: {} {}", itemId, e.statusCode(), e.getMessage());
            throw new BusinessException(ChatErrorCode.LANGFUSE_UNAVAILABLE, "failed to upsert Langfuse golden item");
        } catch (Exception e) {
            log.warn("Langfuse upsert golden item {} failed: {}", itemId, e.getMessage());
            throw new BusinessException(ChatErrorCode.LANGFUSE_UNAVAILABLE, "failed to upsert Langfuse golden item");
        }
    }

    /**
     * @param datasetName Dataset
     * @return items；SDK 不可用时为空
     */
    public List<DatasetItem> listItems(String datasetName) {
        if (!properties.isLangfuseEnable() || !StringUtils.hasText(datasetName)) {
            return List.of();
        }
        LangfuseClient client = requireClient();
        try {
            List<DatasetItem> items = new ArrayList<>();
            int page = 1;
            int totalPages = 1;
            while (page <= totalPages) {
                PaginatedDatasetItems pageResult = client.datasetItems().list(GetDatasetItemsRequest.builder()
                        .datasetName(datasetName).page(page).limit(100).build());
                if (pageResult.getData() != null) {
                    items.addAll(pageResult.getData());
                }
                if (pageResult.getMeta() != null) {
                    totalPages = Math.max(1, pageResult.getMeta().getTotalPages());
                } else {
                    totalPages = page;
                }
                page += 1;
            }
            log.info("Langfuse listed {} items from dataset {}", items.size(), datasetName);
            return items;
        } catch (Exception e) {
            log.warn("Langfuse list dataset items {} failed: {}", datasetName, e.getMessage());
            return List.of();
        }
    }

    /**
     * 把评测分数写到 Langfuse Session（与 OTEL {@code langfuse.session.id} 对齐）。
     * langfuse-java 0.3.0 无 traces list / dataset-run-items，因此不查 trace、不写 Dataset Run。
     *
     * @param sessionId 会话 id
     * @param name      分项名
     * @param value     分数
     * @param comment   依据
     * @param metadata  含 category
     */
    public void createScore(String sessionId, String name, double value, String comment, Map<String, Object> metadata) {
        if (!properties.isLangfuseEnable() || !StringUtils.hasText(sessionId)) {
            return;
        }
        LangfuseClient client = langfuseClient.getIfAvailable();
        if (client == null) {
            log.warn("Langfuse SDK unavailable, skip score {}", name);
            return;
        }
        try {
            var builder = CreateScoreRequest.builder()
                    .name(name)
                    .value(CreateScoreValue.of(value))
                    .sessionId(sessionId)
                    .dataType(ScoreDataType.NUMERIC);
            if (StringUtils.hasText(comment)) {
                builder = builder.comment(comment);
            }
            if (metadata != null) {
                builder = builder.metadata(metadata);
            }
            client.score().create(builder.build());
        } catch (Exception e) {
            log.warn("Langfuse create score {} failed: {}", name, e.getMessage());
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
                    .description(descriptionFor(datasetName))
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

    private static String descriptionFor(String datasetName) {
        if ("pmc-golden".equals(datasetName)) {
            return "PMC agent golden eval tasks with success criteria";
        }
        return "PMC agent thumbs-down base cases";
    }

    private LangfuseClient requireClient() {
        if (!properties.isLangfuseEnable()) {
            throw new BusinessException(ChatErrorCode.LANGFUSE_UNAVAILABLE, "Langfuse is not enabled");
        }
        LangfuseClient client = langfuseClient.getIfAvailable();
        if (client == null) {
            throw new BusinessException(ChatErrorCode.LANGFUSE_UNAVAILABLE, "Langfuse is not enabled");
        }
        return client;
    }

}
