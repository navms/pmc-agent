package io.github.navms.agent.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.navms.agent.eval.config.EvalProperties;
import io.github.navms.agent.eval.records.ExpectedOutput;
import io.github.navms.agent.eval.records.GoldenInput;
import io.github.navms.agent.observability.LangfuseDatasetClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 将 classpath Golden Set seed 到 Langfuse；不走 EvalHarness。
 *
 * @author navms
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "pmc.eval.enabled=false")
class GoldenDatasetSeederTest {

    @Autowired
    private LangfuseDatasetClient langfuseDatasetClient;

    @Autowired
    private EvalProperties evalProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void seedClasspathGolden() throws Exception {
        List<GoldenTask> tasks = classpathTasks();
        assertFalse(tasks.isEmpty());
        int count = seed(evalProperties.getDataset(), tasks, langfuseDatasetClient);
        assertTrue(count > 0);
    }

    /**
     * @param dataset               Dataset 名
     * @param tasks                 带成功标准的任务
     * @param langfuseDatasetClient Langfuse
     * @return 成功 upsert 条数
     */
    private static int seed(String dataset, List<GoldenTask> tasks, LangfuseDatasetClient langfuseDatasetClient) {
        int count = 0;
        for (GoldenTask task : tasks) {
            if (!task.hasCriteria()) {
                continue;
            }
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("seed", true);
            metadata.put("layer", task.layer());
            langfuseDatasetClient.upsertGoldenItem(dataset, task.id(), toInput(task), toExpected(task), metadata);
            count += 1;
        }
        return count;
    }

    private List<GoldenTask> classpathTasks() throws Exception {
        List<GoldenTask> tasks = new ArrayList<>();
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:eval/golden/*.json");
        for (Resource resource : resources) {
            if (!resource.exists() || !resource.isReadable()) {
                continue;
            }
            try (InputStream in = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(in);
                if (root.isArray()) {
                    for (JsonNode item : root) {
                        tasks.add(toTask(objectMapper.treeToValue(item, GoldenDocument.class)));
                    }
                } else if (root.isObject()) {
                    tasks.add(toTask(objectMapper.treeToValue(root, GoldenDocument.class)));
                }
            }
        }
        return tasks;
    }

    private static GoldenTask toTask(GoldenDocument document) {
        GoldenInput input = document.input();
        List<GoldenTurn> turns = input == null ? List.of() : input.replayTurns();
        String title = document.title() != null ? document.title() : (input == null ? document.id() : input.title());
        return new GoldenTask(document.id(), title, document.layer(), turns, document.expectedOutput(), document.trials());
    }

    static Map<String, Object> toInput(GoldenTask task) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("title", task.title());
        input.put("turns", task.turns());
        return input;
    }

    static Map<String, Object> toExpected(GoldenTask task) {
        Map<String, Object> expected = new LinkedHashMap<>();
        ExpectedOutput output = task.expectedOutput();
        if (output == null) {
            return expected;
        }
        expected.put("taskType", output.taskType());
        expected.put("weight", output.weight());
        expected.put("layer", task.layer());
        if (task.trials() != null) {
            expected.put("trials", task.trials());
        }
        expected.put("successCriteria", output.criteria());
        return expected;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoldenDocument(
            String id,
            String title,
            String layer,
            Integer trials,
            GoldenInput input,
            ExpectedOutput expectedOutput) {
    }
}
