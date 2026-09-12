package io.github.navms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AgentScope 运行时配置。
 *
 * @author navms
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "pmc.agent")
public class AgentScopeProperties {

    /**
     * DashScope 模型名。
     */
    private String model = "deepseek-v4-flash-0731";

    /**
     * DashScope API Key。
     */
    private String apiKey;

    /**
     * 同步 spawn 超时提示（秒），写入父 Agent prompt。
     */
    private int spawnTimeoutSeconds = 120;

    /**
     * AgentState 本地目录。
     */
    private String stateDir = "./data/agentscope/state";

    /**
     * Harness workspace 目录。
     */
    private String workspaceDir = "./data/agentscope/workspace";

}
