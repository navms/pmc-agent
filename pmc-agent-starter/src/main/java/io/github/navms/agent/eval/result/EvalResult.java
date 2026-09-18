package io.github.navms.agent.eval.result;

import java.time.Instant;
import java.util.Map;

/**
 * 一次 Trial 的评测记录。
 *
 * @author navms
 */
public record EvalResult(
        String evalId,
        String taskId,
        String trialId,
        String promptVersion,
        String modelId,
        String datasetVersion,
        Instant taskAsOf,
        String timezone,
        String environmentVersion,
        String evidenceSnapshotUri,
        String inputHash,
        String rawInput,
        String referenceOutput,
        String actualOutput,
        String transcriptUri,
        String outcomeStatus,
        Map<String, Double> scores,
        String judgeModel,
        String graderVersion,
        String judgeReasoning,
        String errorCategory,
        String rootCauseLayer,
        Double confidence,
        Instant evaluatedAt) {
}
