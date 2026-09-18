package io.github.navms.agent.eval;

import com.langfuse.client.resources.commons.types.DatasetItem;
import io.agentscope.core.util.JsonUtils;
import io.github.navms.agent.eval.records.ExpectedOutput;
import io.github.navms.agent.eval.records.GoldenInput;
import io.github.navms.agent.observability.dataset.Input;
import io.github.navms.agent.observability.dataset.Metadata;
import org.springframework.util.CollectionUtils;

/**
 * Dataset Items
 *
 * @author navms
 */
public interface DatasetItems {

    /**
     * @return 是否带可验证成功标准（DatasetItem 的 expectedOutput）
     */
    static boolean hasSuccessCriteria(DatasetItem datasetItem) {
        ExpectedOutput expectedOutput = expectedOutputOf(datasetItem);
        return expectedOutput != null && !CollectionUtils.isEmpty(expectedOutput.successCriteria());
    }

    /**
     * @param datasetItem SDK item
     * @return base-case input；无法转换则为 null
     */
    static Input inputOf(DatasetItem datasetItem) {
        if (datasetItem == null || datasetItem.getInput() == null) {
            return null;
        }
        return JsonUtils.getJsonCodec().convertValue(datasetItem.getInput(), Input.class);
    }

    /**
     * @param datasetItem SDK item
     * @return Golden input；无法转换则为 null
     */
    static GoldenInput goldenInputOf(DatasetItem datasetItem) {
        if (datasetItem == null || datasetItem.getInput() == null) {
            return null;
        }
        return JsonUtils.getJsonCodec().convertValue(datasetItem.getInput(), GoldenInput.class);
    }

    /**
     * @param datasetItem SDK item
     * @return expectedOutput；无法转换则为 null
     */
    static ExpectedOutput expectedOutputOf(DatasetItem datasetItem) {
        if (datasetItem == null || datasetItem.getExpectedOutput() == null) {
            return null;
        }
        return JsonUtils.getJsonCodec().convertValue(datasetItem.getExpectedOutput(), ExpectedOutput.class);
    }

    /**
     * @param datasetItem SDK item
     * @return 点踩 metadata；无法转换则为 null
     */
    static Metadata metadataOf(DatasetItem datasetItem) {
        if (datasetItem == null || datasetItem.getMetadata() == null) {
            return null;
        }
        return JsonUtils.getJsonCodec().convertValue(datasetItem.getMetadata(), Metadata.class);
    }

    /**
     * @param datasetItem SDK item
     * @return 是否应按 Golden schema（turns + 成功标准）解析
     */
    static boolean goldenItem(DatasetItem datasetItem) {
        if (hasSuccessCriteria(datasetItem)) {
            return true;
        }
        GoldenInput goldenInput = goldenInputOf(datasetItem);
        return goldenInput != null && !goldenInput.replayTurns().isEmpty();
    }
}
