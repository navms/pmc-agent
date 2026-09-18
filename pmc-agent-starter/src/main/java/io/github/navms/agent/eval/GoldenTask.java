package io.github.navms.agent.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.langfuse.client.resources.commons.types.DatasetItem;
import io.github.navms.agent.eval.records.ExpectedOutput;
import io.github.navms.agent.eval.records.GoldenInput;
import io.github.navms.agent.observability.dataset.Input;

import java.util.ArrayList;
import java.util.List;

/**
 * 一条评测任务。
 *
 * @param id             稳定 Task id，同时作为 Langfuse dataset item id
 * @param title          说明
 * @param layer          normal / edge / adversarial / high
 * @param turns          用户轮次
 * @param expectedOutput 成功标准
 * @param trials         覆盖全局 trials 配置；null 用全局
 * @author navms
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoldenTask(
        String id,
        String title,
        String layer,
        List<GoldenTurn> turns,
        ExpectedOutput expectedOutput,
        Integer trials) {

    /**
     * @return 是否带可执行成功标准
     */
    public boolean hasCriteria() {
        return expectedOutput != null && !expectedOutput.successCriteria().isEmpty();
    }

    /**
     * 从 Langfuse {@link DatasetItem} 转成评测任务。
     * Golden 用 turns；base-case 用 messages 抽 user。
     *
     * @param datasetItem SDK datasetItem
     * @return task；缺字段则为 skip 形态
     */
    public static GoldenTask fromDatasetItem(DatasetItem datasetItem) {
        if (datasetItem == null) {
            return new GoldenTask("", "", "normal", List.of(), null, null);
        }
        if (DatasetItems.goldenItem(datasetItem)) {
            return fromGoldenItem(datasetItem);
        }
        return fromBaseCaseItem(datasetItem);
    }

    private static GoldenTask fromGoldenItem(DatasetItem datasetItem) {
        String id = datasetItem.getId() == null ? "" : datasetItem.getId();
        GoldenInput input = DatasetItems.goldenInputOf(datasetItem);
        ExpectedOutput expectedOutput = DatasetItems.expectedOutputOf(datasetItem);
        List<GoldenTurn> turns = input == null ? List.of() : input.replayTurns();
        String title = input != null && input.title() != null && !input.title().isBlank() ? input.title() : id;
        String layer = expectedOutput != null && expectedOutput.layer() != null ? expectedOutput.layer() : "normal";
        Integer trials = expectedOutput == null ? null : expectedOutput.trials();
        return new GoldenTask(id, title, layer, turns, expectedOutput, trials);
    }

    private static GoldenTask fromBaseCaseItem(DatasetItem datasetItem) {
        String id = datasetItem.getId() == null ? "" : datasetItem.getId();
        Input input = DatasetItems.inputOf(datasetItem);
        List<GoldenTurn> turns = userTurns(input);
        String title = input != null && input.title() != null && !input.title().isBlank() ? input.title() : id;
        return new GoldenTask(id, title, "normal", turns, null, null);
    }

    private static List<GoldenTurn> userTurns(Input input) {
        if (input == null) {
            return List.of();
        }
        List<GoldenTurn> turns = new ArrayList<>();
        for (Input.Message message : input.userTurns()) {
            turns.add(new GoldenTurn("user", message.content().trim()));
        }
        return List.copyOf(turns);
    }

}
