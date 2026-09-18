package io.github.navms.agent.eval.records;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Expected payload
 *
 * @author navms
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpectedOutput(
        String taskType,
        String weight,
        String layer,
        Integer trials,
        List<SuccessCriterion> successCriteria) {

    /**
     * Golden JSON / 单测常用形态。
     *
     * @param taskType         场景类型
     * @param weight           normal / high
     * @param successCriteria  成功标准
     */
    public ExpectedOutput(String taskType, String weight, List<SuccessCriterion> successCriteria) {
        this(taskType, weight, null, null, successCriteria);
    }

    /**
     * @return 是否高风险 Task
     */
    public boolean highWeight() {
        return weight != null && "high".equalsIgnoreCase(weight.trim());
    }

    /**
     * @return 非空标准列表
     */
    public List<SuccessCriterion> criteria() {
        return successCriteria == null ? List.of() : successCriteria;
    }

}