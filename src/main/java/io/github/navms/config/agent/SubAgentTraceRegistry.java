package io.github.navms.config.agent;

import io.github.navms.web.agent.vo.message.MessageResponse;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 子 Agent 内部 tool-request / tool 轨迹的旁路注册表。
 * <p>
 * TracingAgentToolCallback 在子 Agent 执行结束后写入；
 * ChatAgentAppService 在展开 AgentTool 的 tool 结果时取出并清除。
 *
 * @author navms
 */
public final class SubAgentTraceRegistry {

    private static final ConcurrentHashMap<String, ConcurrentLinkedDeque<List<MessageResponse>>> BY_AGENT =
            new ConcurrentHashMap<>();

    private SubAgentTraceRegistry() {
    }

    /**
     * @param agentName 子 Agent 名（与 AgentTool 名相同）
     * @param events    嵌套消息（tool-request / tool）
     */
    public static void push(String agentName, List<MessageResponse> events) {
        if (agentName == null || events == null || events.isEmpty()) {
            return;
        }
        BY_AGENT.computeIfAbsent(agentName, key -> new ConcurrentLinkedDeque<>()).addLast(List.copyOf(events));
    }

    /**
     * 取出并移除该 Agent 最早一条轨迹；无则返回空列表。
     *
     * @param agentName 子 Agent 名
     * @return 嵌套消息
     */
    public static List<MessageResponse> poll(String agentName) {
        if (agentName == null) {
            return List.of();
        }
        ConcurrentLinkedDeque<List<MessageResponse>> queue = BY_AGENT.get(agentName);
        if (queue == null) {
            return List.of();
        }
        List<MessageResponse> events = queue.pollFirst();
        if (queue.isEmpty()) {
            BY_AGENT.remove(agentName, queue);
        }
        return events == null ? List.of() : events;
    }
}
