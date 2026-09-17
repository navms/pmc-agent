package io.github.navms.agent.prompt;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 名到当前生效 prompt 版本的映射，供 OTEL span 绑定 Langfuse Prompt。
 *
 * @author navms
 */
public class LangfusePromptRegistry {

    private final ConcurrentHashMap<String, LangfusePromptSnapshot> byAgent = new ConcurrentHashMap<>();

    /**
     * @param agentName AgentScope 名
     * @param snapshot  当前快照
     */
    public void put(String agentName, LangfusePromptSnapshot snapshot) {
        if (agentName != null && snapshot != null) {
            byAgent.put(agentName, snapshot);
        }
    }

    /**
     * @param agentName AgentScope 名
     * @return 已注册快照
     */
    public Optional<LangfusePromptSnapshot> find(String agentName) {
        return Optional.ofNullable(byAgent.get(agentName));
    }

}
