package io.github.navms.agent.eval;

import io.github.navms.agent.eval.records.ExpectedOutput;
import io.github.navms.agent.eval.records.SuccessCriterion;
import io.github.navms.agent.eval.result.GradeResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 规则 Grader：对照成功标准检查 Transcript，不调用 LLM。
 *
 * @author navms
 */
public final class RuleGrader {

    /**
     * @param expected     成功标准
     * @param observation  观察
     * @return 评分
     */
    public GradeResult grade(ExpectedOutput expected, TranscriptObservation observation) {
        if (observation != null && observation.failed()) {
            return harnessFailure(observation.harnessError());
        }
        List<SuccessCriterion> criteria = expected == null ? List.of() : expected.criteria();
        if (criteria.isEmpty()) {
            return new GradeResult(
                    false,
                    "HARNESS_SKIP",
                    Map.of("task_complete", 0.0),
                    List.of(),
                    "missing_criteria",
                    "HARNESS",
                    "item 无 successCriteria，跳过回归打分");
        }
        TranscriptObservation seen = observation == null ? TranscriptObservation.empty() : observation;
        List<CriterionResult> results = new ArrayList<>();
        for (SuccessCriterion criterion : criteria) {
            results.add(gradeOne(criterion, seen));
        }
        Set<String> required = requiredTools(criteria);
        Set<String> actual = seen.actualTools();
        double precision = precision(required, actual);
        double recall = recall(required, actual);
        double setMatch = actual.equals(required) ? 1.0 : 0.0;
        double unnecessary = unnecessaryRate(required, actual);
        boolean allPass = results.stream().allMatch(CriterionResult::passed);
        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("task_complete", allPass ? 1.0 : 0.0);
        scores.put("tool_precision", precision);
        scores.put("tool_recall", recall);
        scores.put("tool_set_match", setMatch);
        scores.put("unnecessary_call_rate", unnecessary);
        CriterionResult firstFail = results.stream().filter(item -> !item.passed()).findFirst().orElse(null);
        String category = firstFail == null ? "" : firstFail.category();
        String reason = firstFail == null
                ? "全部成功标准通过"
                : firstFail.reason();
        return new GradeResult(
                allPass,
                allPass ? "SUCCESS" : "FAILED",
                scores,
                results,
                category,
                "AGENT",
                reason);
    }

    private static GradeResult harnessFailure(String error) {
        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("task_complete", 0.0);
        scores.put("tool_precision", 0.0);
        scores.put("tool_recall", 0.0);
        scores.put("tool_set_match", 0.0);
        scores.put("unnecessary_call_rate", 0.0);
        return new GradeResult(false, "HARNESS_ERROR", scores, List.of(), "harness_error", "HARNESS", error);
    }

    static CriterionResult gradeOne(SuccessCriterion criterion, TranscriptObservation observation) {
        String kind = criterion.normalizedKind();
        return switch (kind) {
            case "required_agent" -> requireContains(
                    criterion, observation.subAgents(), "tool_missing", "未 spawn 必要子 Agent ");
            case "forbidden_agent" -> requireAbsent(
                    criterion, observation.subAgents(), "wrong_agent", "误 spawn 禁止子 Agent ");
            case "required_write_tool" -> requireContains(
                    criterion, observation.writeTools(), "tool_missing", "未调用必要写工具 ");
            case "forbidden_write_tool" -> requireAbsent(
                    criterion, observation.writeTools(), "wrong_tool", "调用了禁止写工具 ");
            case "interrupt" -> gradeInterrupt(criterion, observation);
            case "clarify_only" -> gradeClarifyOnly(criterion, observation);
            case "required_clarify" -> {
                boolean ok = observation.clarified();
                yield new CriterionResult(
                        criterion.id(),
                        ok,
                        ok ? 1.0 : 0.0,
                        ok ? "已出现意图澄清" : "未出现意图澄清",
                        ok ? "" : "clarify_miss");
            }
            default -> new CriterionResult(
                    criterion.id(), false, 0.0, "未知 kind: " + criterion.kind(), "invalid_criterion");
        };
    }

    private static CriterionResult gradeInterrupt(SuccessCriterion criterion, TranscriptObservation observation) {
        Boolean expected = criterion.boolValue();
        if (expected == null) {
            return new CriterionResult(criterion.id(), false, 0.0, "interrupt 值无法解析", "invalid_criterion");
        }
        boolean actual = observation.interrupted();
        boolean ok = expected == actual;
        String category = "";
        if (!ok) {
            category = expected ? "hitl_skipped" : "hitl_unexpected";
        }
        return new CriterionResult(
                criterion.id(),
                ok,
                ok ? 1.0 : 0.0,
                "期望 interrupt=" + expected + " 实际=" + actual,
                category);
    }

    private static CriterionResult gradeClarifyOnly(SuccessCriterion criterion, TranscriptObservation observation) {
        boolean spawned = !observation.subAgents().isEmpty();
        boolean wrote = !observation.writeTools().isEmpty();
        boolean ok = !spawned && !wrote;
        return new CriterionResult(
                criterion.id(),
                ok,
                ok ? 1.0 : 0.0,
                ok ? "本轮未 spawn、未写工具" : "澄清轮仍调用了子 Agent 或写工具",
                ok ? "" : "clarify_miss");
    }

    private static CriterionResult requireContains(
            SuccessCriterion criterion, Set<String> actual, String missCategory, String prefix) {
        String expected = criterion.stringValue();
        boolean ok = actual != null && actual.contains(expected);
        return new CriterionResult(
                criterion.id(),
                ok,
                ok ? 1.0 : 0.0,
                ok ? "命中 " + expected : prefix + expected + "，实际=" + actual,
                ok ? "" : missCategory);
    }

    private static CriterionResult requireAbsent(
            SuccessCriterion criterion, Set<String> actual, String category, String prefix) {
        String forbidden = criterion.stringValue();
        boolean hit = actual != null && actual.contains(forbidden);
        boolean ok = !hit;
        return new CriterionResult(
                criterion.id(),
                ok,
                ok ? 1.0 : 0.0,
                ok ? "未出现 " + forbidden : prefix + forbidden,
                ok ? "" : category);
    }

    static Set<String> requiredTools(List<SuccessCriterion> criteria) {
        LinkedHashSet<String> required = new LinkedHashSet<>();
        for (SuccessCriterion criterion : criteria) {
            String kind = criterion.normalizedKind();
            if ("required_agent".equals(kind) || "required_write_tool".equals(kind)) {
                String value = criterion.stringValue();
                if (!value.isEmpty()) {
                    required.add(value);
                }
            }
        }
        return required;
    }

    static double precision(Set<String> required, Set<String> actual) {
        if (actual == null || actual.isEmpty()) {
            return required == null || required.isEmpty() ? 1.0 : 0.0;
        }
        long hit = actual.stream().filter(required::contains).count();
        return hit / (double) actual.size();
    }

    static double recall(Set<String> required, Set<String> actual) {
        if (required == null || required.isEmpty()) {
            return 1.0;
        }
        long hit = required.stream().filter(actual::contains).count();
        return hit / (double) required.size();
    }

    static double unnecessaryRate(Set<String> required, Set<String> actual) {
        if (actual == null || actual.isEmpty()) {
            return 0.0;
        }
        long extra = actual.stream().filter(item -> !required.contains(item)).count();
        return extra / (double) actual.size();
    }
}
