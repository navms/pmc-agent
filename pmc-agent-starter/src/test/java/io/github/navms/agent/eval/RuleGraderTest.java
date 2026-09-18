package io.github.navms.agent.eval;

import io.github.navms.agent.eval.records.ExpectedOutput;
import io.github.navms.agent.eval.records.SuccessCriterion;
import io.github.navms.agent.eval.result.GradeResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleGraderTest {

    private final RuleGrader grader = new RuleGrader();

    @Test
    void summarizeRequiresAgentForbidsQuery() {
        ExpectedOutput expected = new ExpectedOutput("summarize", "normal", List.of(
                new SuccessCriterion("spawn_summarize", "required_agent", "summarize_bank"),
                new SuccessCriterion("forbid_query", "forbidden_agent", "query_bank"),
                new SuccessCriterion("no_interrupt", "interrupt", false)));
        TranscriptObservation ok = new TranscriptObservation(
                Set.of("summarize_bank"), Set.of(), false, List.of(), true, "合计 10", null, 10L);
        GradeResult pass = grader.grade(expected, ok);
        assertTrue(pass.passed());
        assertEquals(1.0, pass.scores().get("task_complete"));
        assertEquals(1.0, pass.scores().get("tool_set_match"));

        TranscriptObservation wrong = new TranscriptObservation(
                Set.of("query_bank"), Set.of(), false, List.of(), true, "十条样本", null, 10L);
        GradeResult fail = grader.grade(expected, wrong);
        assertFalse(fail.passed());
        assertEquals("tool_missing", fail.errorCategory());
        assertEquals("AGENT", fail.rootCauseLayer());
    }

    @Test
    void highRiskWriteMustInterrupt() {
        ExpectedOutput expected = new ExpectedOutput("write", "high", List.of(
                new SuccessCriterion("pay", "required_write_tool", "submitBankPayOrder"),
                new SuccessCriterion("hitl", "interrupt", true)));
        GradeResult miss = grader.grade(expected, new TranscriptObservation(
                Set.of(), Set.of("submitBankPayOrder"), false, List.of(), false, "已提交", null, 1L));
        assertFalse(miss.passed());
        assertEquals("hitl_skipped", miss.errorCategory());

        GradeResult hit = grader.grade(expected, new TranscriptObservation(
                Set.of(), Set.of("submitBankPayOrder"), true, List.of("t1"), false, "", null, 1L));
        assertTrue(hit.passed());
    }

    @Test
    void clarifyOnlyForbidsSpawn() {
        ExpectedOutput expected = new ExpectedOutput("clarify", "edge", List.of(
                new SuccessCriterion("ask", "required_clarify", true),
                new SuccessCriterion("only", "clarify_only", true)));
        GradeResult pass = grader.grade(expected, new TranscriptObservation(
                Set.of(), Set.of(), false, List.of(), true, "请问账号？", null, 1L));
        assertTrue(pass.passed());
        GradeResult fail = grader.grade(expected, new TranscriptObservation(
                Set.of("query_bank"), Set.of(), false, List.of(), true, "查了", null, 1L));
        assertFalse(fail.passed());
        assertEquals("clarify_miss", fail.errorCategory());
    }

    @Test
    void chatHasEmptyRequiredSet() {
        ExpectedOutput expected = new ExpectedOutput("chat", "normal", List.of(
                new SuccessCriterion("no_query", "forbidden_agent", "query_bank"),
                new SuccessCriterion("no_sum", "forbidden_agent", "summarize_bank"),
                new SuccessCriterion("no_export", "forbidden_agent", "export_excel"),
                new SuccessCriterion("no_chart", "forbidden_agent", "create_chart")));
        GradeResult pass = grader.grade(expected, TranscriptObservation.empty());
        assertTrue(pass.passed());
        assertEquals(1.0, pass.scores().get("tool_precision"));
        assertEquals(1.0, pass.scores().get("tool_recall"));
    }

    @Test
    void missingCriteriaIsHarnessSkip() {
        GradeResult result = grader.grade(new ExpectedOutput("x", "normal", List.of()), TranscriptObservation.empty());
        assertEquals("HARNESS_SKIP", result.outcomeStatus());
        assertEquals("HARNESS", result.rootCauseLayer());
    }

    @Test
    void harnessErrorNotAgentFailure() {
        GradeResult result = grader.grade(
                new ExpectedOutput("x", "normal", List.of(new SuccessCriterion("a", "required_agent", "query_bank"))),
                new TranscriptObservation(Set.of(), Set.of(), false, List.of(), false, "", "timeout", 1L));
        assertEquals("HARNESS_ERROR", result.outcomeStatus());
        assertEquals("HARNESS", result.rootCauseLayer());
    }

    @Test
    void toolMetrics() {
        assertEquals(1.0, RuleGrader.recall(Set.of(), Set.of("query_bank")));
        assertEquals(0.0, RuleGrader.precision(Set.of("summarize_bank"), Set.of("query_bank")));
        assertEquals(0.5, RuleGrader.unnecessaryRate(Set.of("summarize_bank"), Set.of("summarize_bank", "query_bank")));
    }
}
