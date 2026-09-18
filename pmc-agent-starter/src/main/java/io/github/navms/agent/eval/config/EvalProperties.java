package io.github.navms.agent.eval.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agent Eval Harness 开关与运行参数。
 *
 * @author navms
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "pmc.eval")
public class EvalProperties {

    /**
     * 为 true 时启动后跑评测并退出。
     */
    private boolean enabled = false;

    /**
     * Golden Dataset 名。
     */
    private String dataset = "golden-case";

    /**
     * Dataset Run 名，空则按时间生成。
     */
    private String runName = "";

    /**
     * 最多评几条，0 表示全部。
     */
    private int maxItems = 0;

    /**
     * 单轮 AG-UI 超时秒数。
     */
    private int timeoutSeconds = 180;

    /**
     * 默认 Trial 次数。
     */
    private int trials = 1;

    /**
     * HITL resume：deny / approve / none。
     */
    private String hitlResume = "approve";

    /**
     * 汇总 JSON 输出目录。
     */
    private String outputDir = "./data/eval";

    /**
     * 评测用户。
     */
    private String userId = "eval-harness";
}
