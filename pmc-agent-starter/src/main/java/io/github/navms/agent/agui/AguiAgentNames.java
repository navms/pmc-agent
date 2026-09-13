package io.github.navms.agent.agui;

import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * AG-UI source / 工具名展示规则。
 *
 * @author navms
 */
public final class AguiAgentNames {

    public static final String PARENT_AGENT = "pmc_supervisor";

    public static final String GENERAL_CHAT = "general_chat";

    static final Set<String> HIDDEN_PARENT_TOOLS = Set.of(
            "agent_spawn",
            "agent_send",
            "agent_list",
            "task_output",
            "wait_async_results",
            "task_cancel",
            "task_list");

    /**
     * @param source AgentEvent.source 或 CUSTOM.value.source
     * @return 展示用 agent 名
     */
    public static String fromSource(String source) {
        if (!StringUtils.hasText(source)) {
            return GENERAL_CHAT;
        }
        String segment = source;
        int slash = source.lastIndexOf('/');
        if (slash >= 0 && slash < source.length() - 1) {
            segment = source.substring(slash + 1);
        }
        if (PARENT_AGENT.equals(segment)) {
            return GENERAL_CHAT;
        }
        return segment;
    }

    /**
     * @param agentName 展示名
     * @param toolName  工具名
     * @return 是否隐藏父调度工具
     */
    public static boolean hideParentTool(String agentName, String toolName) {
        return (GENERAL_CHAT.equals(agentName) || PARENT_AGENT.equals(agentName))
                && toolName != null
                && HIDDEN_PARENT_TOOLS.contains(toolName);
    }
}
