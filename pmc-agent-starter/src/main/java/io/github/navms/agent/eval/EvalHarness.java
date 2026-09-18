package io.github.navms.agent.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.navms.agent.AgentScopeProperties;
import io.github.navms.agent.eval.config.EvalProperties;
import io.github.navms.agent.eval.loader.GoldenTaskLoader;
import io.github.navms.agent.eval.result.EvalResult;
import io.github.navms.agent.eval.result.EvalRunSummary;
import io.github.navms.agent.eval.result.GradeResult;
import io.github.navms.agent.observability.LangfuseDatasetClient;
import io.github.navms.agent.prompt.LangfusePromptRegistry;
import io.github.navms.application.chat.dto.ChatSessionInfo;
import io.github.navms.application.chat.dto.CreateSessionCommand;
import io.github.navms.application.chat.service.ChatSessionAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 读 Golden Set、隔离 Trial、调用 AG-UI、规则评分、写 Langfuse Dataset Run。
 *
 * @author navms
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvalHarness {

    static final String GRADER_VERSION = "rule-grader-v1";

    private final EvalProperties properties;

    private final AgentScopeProperties agentScopeProperties;

    private final GoldenTaskLoader taskLoader;

    private final ChatSessionAppService chatSessionAppService;

    private final AguiReplayClient aguiReplayClient;

    private final LangfuseDatasetClient langfuseDatasetClient;

    private final LangfusePromptRegistry promptRegistry;

    private final ApplicationContext applicationContext;

    private final ObjectMapper objectMapper;

    private final RuleGrader ruleGrader = new RuleGrader();

    /**
     * @return 进程退出码：0 通过，1 高权重失败，2 配置/Harness 错误
     */
    public int execute() {
        Instant started = Instant.now();
        List<GoldenTask> tasks = taskLoader.load(properties.getDataset());
        if (properties.getMaxItems() > 0 && tasks.size() > properties.getMaxItems()) {
            tasks = List.copyOf(tasks.subList(0, properties.getMaxItems()));
        }

        String runName = StringUtils.hasText(properties.getRunName())
                ? properties.getRunName()
                : "pmc-eval-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

        String modelId = agentScopeProperties.getModel();
        String promptVersion = promptRegistry.find("pmc_supervisor")
                .map(snapshot -> snapshot.name() + "@" + snapshot.version())
                .orElse("pmc/supervisor@fallback");

        String baseUrl = localBaseUrl();
        Duration timeout = Duration.ofSeconds(Math.max(10, properties.getTimeoutSeconds()));

        List<EvalResult> evalResults = new ArrayList<>();
        int skipped = 0;
        int errors = 0;
        int highWeightFailures = 0;
        Map<String, Integer> passCount = new LinkedHashMap<>();
        Map<String, Integer> scoreCount = new LinkedHashMap<>();
        Map<String, Double> scoreSum = new LinkedHashMap<>();
        Map<String, Integer> taskPassTrials = new LinkedHashMap<>();
        Map<String, Integer> taskTrialTotal = new LinkedHashMap<>();

        for (GoldenTask task : tasks) {
            if (CollectionUtils.isEmpty(task.turns())) {
                skipped += 1;
                continue;
            }
            if (!task.hasCriteria()) {
                skipped += 1;
                log.info("Skip {} : missing successCriteria", task.id());
                continue;
            }
            int trials = task.trials() != null ? Math.max(1, task.trials()) : Math.max(1, properties.getTrials());
            int successTrials = 0;

            for (int trialIndex = 1; trialIndex <= trials; trialIndex++) {
                EvalResult evalResult =
                        this.runTrial(task, trialIndex, runName, modelId, promptVersion, baseUrl, timeout);

                evalResults.add(evalResult);
                if ("HARNESS_SKIP".equals(evalResult.outcomeStatus())) {
                    skipped += 1;
                } else if ("HARNESS_ERROR".equals(evalResult.outcomeStatus())) {
                    errors += 1;
                }
                boolean passed = evalResult.scores() != null && evalResult.scores().getOrDefault("task_complete", 0.0) >= 1.0;
                if (passed) {
                    successTrials += 1;
                } else if (task.expectedOutput() != null && task.expectedOutput().highWeight()
                        && !"HARNESS_ERROR".equals(evalResult.outcomeStatus())
                        && !"HARNESS_SKIP".equals(evalResult.outcomeStatus())) {
                    highWeightFailures += 1;
                }

                if (evalResult.scores() != null) {
                    for (Map.Entry<String, Double> entry : evalResult.scores().entrySet()) {
                        if ("e2e_latency_ms".equals(entry.getKey())) {
                            scoreSum.merge(entry.getKey(), entry.getValue(), Double::sum);
                            scoreCount.merge(entry.getKey(), 1, Integer::sum);
                            continue;
                        }
                        scoreSum.merge(entry.getKey(), entry.getValue(), Double::sum);
                        scoreCount.merge(entry.getKey(), 1, Integer::sum);
                        if (entry.getValue() != null && entry.getValue() >= 1.0) {
                            passCount.merge(entry.getKey(), 1, Integer::sum);
                        }
                    }
                }
            }
            taskPassTrials.put(task.id(), successTrials);
            taskTrialTotal.put(task.id(), trials);
        }

        Instant completed = Instant.now();
        Map<String, Double> avg = average(scoreSum, scoreCount);
        Map<String, Double> passRates = rate(passCount, scoreCount);
        Map<String, Double> passAtK = new LinkedHashMap<>();
        Map<String, Double> passHatK = new LinkedHashMap<>();
        int kTasks = 0;
        int atLeastOne = 0;
        int allTrials = 0;
        for (GoldenTask task : tasks) {
            if (!task.hasCriteria()) {
                continue;
            }
            kTasks += 1;
            int ok = taskPassTrials.getOrDefault(task.id(), 0);
            int total = taskTrialTotal.getOrDefault(task.id(), 1);
            if (ok > 0) {
                atLeastOne += 1;
            }
            if (ok == total && total > 0) {
                allTrials += 1;
            }
        }
        if (kTasks > 0) {
            passAtK.put("pass@k", atLeastOne / (double) kTasks);
            passHatK.put("pass^k", allTrials / (double) kTasks);
        }
        boolean passedRegression = highWeightFailures == 0 && errors == 0;
        List<String> details = new ArrayList<>();
        if (highWeightFailures > 0) {
            details.add("highWeightFailures=" + highWeightFailures);
        }
        if (errors > 0) {
            details.add("errors=" + errors);
        }
        EvalRunSummary summary = new EvalRunSummary(
                runName,
                promptVersion,
                modelId,
                properties.getDataset(),
                evalResults.size(),
                skipped,
                errors,
                highWeightFailures,
                avg,
                passRates,
                passAtK,
                passHatK,
                Map.of(),
                passedRegression,
                details,
                started,
                completed);

        writeOutput(runName, summary, evalResults);
        log.info(
                "Eval {} complete cases={} skip={} harnessErr={} highFail={} task_complete_avg={}",
                runName,
                evalResults.size(),
                skipped,
                errors,
                highWeightFailures,
                avg.getOrDefault("task_complete", 0.0));
        if (highWeightFailures > 0) {
            return 1;
        }
        return 0;
    }

    private EvalResult runTrial(
            GoldenTask task,
            int trialIndex,
            String runName,
            String modelId,
            String promptVersion,
            String baseUrl,
            Duration timeout) {
        String evalId = runName + ":" + task.id() + ":" + trialIndex;
        String trialId = task.id() + "-t" + trialIndex;
        Instant evaluatedAt = Instant.now();
        long started = System.currentTimeMillis();
        ChatSessionInfo session;

        try {
            session = chatSessionAppService.create(new CreateSessionCommand(properties.getUserId()));
        } catch (Exception e) {
            return buildEvalResult(task, evalId, trialId, modelId, promptVersion, "", null,
                    new GradeResult(false, "HARNESS_ERROR", Map.of("task_complete", 0.0), List.of(),
                            "harness_error", "HARNESS", "create session: " + e.getMessage()),
                    null, evaluatedAt, System.currentTimeMillis() - started);
        }

        List<TranscriptObservation> observations = new ArrayList<>();
        try {
            for (GoldenTurn turn : task.turns()) {
                if (turn == null || !"user".equalsIgnoreCase(
                        !StringUtils.hasText(turn.role()) ? "user" : turn.role()) || !StringUtils.hasText(turn.content())) {
                    continue;
                }
                TranscriptObservation observation = aguiReplayClient.sendUser(
                        baseUrl, session.id(), properties.getUserId(), turn.content(), timeout);
                observations.add(observation);
                if (observation.failed()) {
                    break;
                }
                if (observation.interrupted() && shouldResume()) {
                    boolean approved = "approve".equalsIgnoreCase(properties.getHitlResume());
                    List<String> ids = observation.interruptIds();
                    if (!CollectionUtils.isEmpty(ids)) {
                        observations.add(aguiReplayClient.resume(
                                baseUrl, session.id(), properties.getUserId(), ids, approved, timeout));
                    }
                }
            }
        } catch (Exception e) {
            observations.add(new TranscriptObservation(
                    Set.of(), Set.of(), false, List.of(), false, "", e.getMessage(), 0L));
        }

        TranscriptObservation observation = TranscriptObservation.merge(observations, System.currentTimeMillis() - started);
        log.info("observation complete {}", observation);

        GradeResult grade = ruleGrader.grade(task.expectedOutput(), observation);
        String sessionKey = String.valueOf(session.id());
        if (grade.scores() != null) {
            for (Map.Entry<String, Double> score : grade.scores().entrySet()) {
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("runName", runName);
                meta.put("taskId", task.id());
                meta.put("category", grade.errorCategory());
                meta.put("reason", grade.reason());
                langfuseDatasetClient.createScore(sessionKey, score.getKey(), score.getValue(), grade.reason(), meta);
            }
        }
        return buildEvalResult(task, evalId, trialId, modelId, promptVersion, sessionKey, observation, grade, sessionKey, evaluatedAt, observation.latencyMs());
    }

    private EvalResult buildEvalResult(
            GoldenTask task,
            String evalId,
            String trialId,
            String modelId,
            String promptVersion,
            String sessionId,
            TranscriptObservation observation,
            GradeResult grade,
            String traceId,
            Instant evaluatedAt,
            long latencyMs) {
        String raw = task.turns() == null ? "" : task.turns().toString();
        Map<String, Double> scores = new LinkedHashMap<>(grade.scoreMap());
        scores.put("e2e_latency_ms", (double) latencyMs);
        return new EvalResult(
                evalId,
                task.id(),
                trialId,
                promptVersion,
                modelId,
                properties.getDataset(),
                null,
                "Asia/Shanghai",
                "bank-seed",
                traceId,
                DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8)),
                raw,
                String.valueOf(task.expectedOutput()),
                observation == null ? "" : observation.assistantText(),
                sessionId,
                grade.outcomeStatus(),
                scores,
                "none",
                GRADER_VERSION,
                grade.reason(),
                grade.errorCategory(),
                grade.rootCauseLayer(),
                1.0,
                evaluatedAt);
    }

    private boolean shouldResume() {
        String mode = properties.getHitlResume();
        return "deny".equalsIgnoreCase(mode) || "approve".equalsIgnoreCase(mode);
    }

    private String localBaseUrl() {
        int port = 8080;
        if (applicationContext instanceof WebServerApplicationContext web) {
            port = web.getWebServer().getPort();
        }
        return "http://127.0.0.1:" + port;
    }

    private static Map<String, Double> average(Map<String, Double> sum, Map<String, Integer> count) {
        Map<String, Double> avg = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : sum.entrySet()) {
            int n = count.getOrDefault(entry.getKey(), 1);
            avg.put(entry.getKey(), n == 0 ? 0.0 : entry.getValue() / n);
        }
        return avg;
    }

    private static Map<String, Double> rate(Map<String, Integer> pass, Map<String, Integer> count) {
        Map<String, Double> rates = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : count.entrySet()) {
            if ("e2e_latency_ms".equals(entry.getKey())) {
                continue;
            }
            int n = entry.getValue();
            rates.put(entry.getKey(), n == 0 ? 0.0 : pass.getOrDefault(entry.getKey(), 0) / (double) n);
        }
        return rates;
    }

    private void writeOutput(String runName, EvalRunSummary summary, List<EvalResult> records) {
        try {
            Path dir = Path.of(properties.getOutputDir());
            Files.createDirectories(dir);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("summary", summary);
            payload.put("records", records);
            Path file = dir.resolve(runName + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), payload);
            log.info("Wrote eval report {}", file.toAbsolutePath());
        } catch (Exception e) {
            log.warn("Failed to write eval report: {}", e.getMessage());
        }
    }

}
