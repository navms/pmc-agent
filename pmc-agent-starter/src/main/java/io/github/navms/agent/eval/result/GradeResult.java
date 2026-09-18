package io.github.navms.agent.eval.result;

import io.github.navms.agent.eval.CriterionResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一条 Trial 的规则评分结果。
 *
 * @param passed              全部标准通过且非 Harness 失败
 * @param outcomeStatus       SUCCESS / FAILED / HARNESS_SKIP / HARNESS_ERROR
 * @param scores              维度分
 * @param criteria            分项
 * @param errorCategory       主失败现象
 * @param rootCauseLayer      AGENT / HARNESS / ENV
 * @param reason              总依据
 * @author navms
 */
public record GradeResult(
        boolean passed,
        String outcomeStatus,
        Map<String, Double> scores,
        List<CriterionResult> criteria,
        String errorCategory,
        String rootCauseLayer,
        String reason) {

    /**
     * @return 可写入 EvalRecord 的分数字典拷贝
     */
    public Map<String, Double> scoreMap() {
        return scores == null ? Map.of() : new LinkedHashMap<>(scores);
    }

}
