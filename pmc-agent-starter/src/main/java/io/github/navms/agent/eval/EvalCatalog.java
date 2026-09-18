package io.github.navms.agent.eval;

import java.util.Set;

/**
 * 评测用到的子 Agent / 写工具名。
 *
 * @author navms
 */
public final class EvalCatalog {

    public static final Set<String> BUSINESS_AGENTS = Set.of(
            "query_bank",
            "summarize_bank",
            "export_excel",
            "create_chart");

    public static final Set<String> WRITE_TOOLS = Set.of(
            "submitBankPayOrder",
            "syncTradeDetails",
            "syncBalanceFlows",
            "syncElectronicReceipts",
            "syncElectronicStatements");

    public static final String SPAWN_TOOL = "agent_spawn";

    /**
     * @param name 工具或 Agent 名
     * @return 是否业务子 Agent
     */
    public static boolean isBusinessAgent(String name) {
        return name != null && BUSINESS_AGENTS.contains(name);
    }

    /**
     * @param name 工具名
     * @return 是否父 Agent 写工具
     */
    public static boolean isWriteTool(String name) {
        return name != null && WRITE_TOOLS.contains(name);
    }
}
