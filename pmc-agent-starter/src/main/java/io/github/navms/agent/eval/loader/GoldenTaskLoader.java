package io.github.navms.agent.eval.loader;

import io.github.navms.agent.eval.GoldenTask;
import io.github.navms.agent.observability.LangfuseDatasetClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 从 classpath 与 Langfuse 读取 Golden Task。
 *
 * @author navms
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoldenTaskLoader {

    private final LangfuseDatasetClient langfuseDatasetClient;

    /**
     * @param dataset Langfuse dataset
     * @return 远端任务（无标准的仍返回，由 Harness skip）
     */
    public List<GoldenTask> load(String dataset) {
        if (langfuseDatasetClient == null) {
            return List.of();
        }
        return langfuseDatasetClient.listItems(dataset).stream()
                .filter(item -> StringUtils.hasText(item.getId())).map(GoldenTask::fromDatasetItem).toList();
    }

}
