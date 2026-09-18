package io.github.navms.agent.eval.result;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 一次评测运行汇总。
 *
 * @author navms
 */
public record EvalRunSummary(
        String runId,
        String promptVersion,
        String modelId,
        String datasetVersion,
        int totalCases,
        int skippedCases,
        int harnessErrors,
        int highWeightFailures,
        Map<String, Double> avgScores,
        Map<String, Double> passRates,
        Map<String, Double> passAtK,
        Map<String, Double> passHatK,
        Map<String, Double> baselineScores,
        boolean passedRegression,
        List<String> regressionDetails,
        Instant startedAt,
        Instant completedAt) {
}
