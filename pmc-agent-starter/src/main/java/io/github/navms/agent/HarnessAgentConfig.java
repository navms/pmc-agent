package io.github.navms.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolkitConfig;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.mysql.MysqlDistributedStore;
import io.agentscope.harness.agent.DistributedStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.filesystem.spec.RemoteFilesystemSpec;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import io.agentscope.harness.agent.memory.compaction.ToolResultEvictionConfig;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import io.agentscope.spring.boot.agui.common.AguiAgentId;
import io.github.navms.agent.middle.TimingMiddleware;
import io.github.navms.agent.tool.BankAggregateTools;
import io.github.navms.agent.tool.BankQueryTools;
import io.github.navms.agent.tool.BankWriteTools;
import io.github.navms.agent.tool.ChartTools;
import io.github.navms.agent.tool.ExcelExportTools;
import io.github.navms.application.chat.hitl.WritePermissionResumeMiddleware;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * HarnessAgent 父调度 + 四个只读/导出子 Agent；写操作工具挂在父 Agent 上并走 ASK HITL。
 *
 * @author navms
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AgentScopeProperties.class)
public class HarnessAgentConfig {

    private static final String QUERY_PROMPT = """
            你是医院业财银企直连系统的明细查询助手。
            只处理点查：账户、支付单号、交易明细号、回单号、最近少量流水等。
            必须通过工具获取数据，严禁编造。未传租户时默认 T001。
            工具最多返回 10 条明细，不要用本 Agent 做合计、趋势、占比或导出。
            工具返回后必须用自然语言向用户汇总结果（账号、笔数、金额、时间等要点）。
            当前日期：%s
            """;

    private static final String SUMMARY_PROMPT = """
            你是医院业财银企直连系统的汇总分析助手。
            只使用汇总工具（SQL 聚合）回答合计、笔数、占比、分组统计等问题。
            按主题选择工具：交易明细、支付单、支付明细单、账户、电子回单、电子对账单。
            严禁根据明细样本推算全量，严禁编造数字。未传租户时默认 T001。
            当前日期：%s
            """;

    private static final String EXCEL_PROMPT = """
            你是医院业财银企直连系统的 Excel 导出助手。
            根据用户条件直接调用导出工具生成文件，返回 downloadUrl 与行数。
            不要先查 10 条明细再导出。不要把表格单元格内容复述给用户，提示下载即可。
            未传租户时默认 T001。当前日期：%s
            """;

    private static final String CHART_PROMPT = """
            你是医院业财银企直连系统的图表助手。
            使用图表工具基于 SQL 聚合生成 ECharts option。
            不要用明细点查结果画图，不要编造数据点。未传租户时默认 T001。
            当前日期：%s
            """;

    private static final String SUPERVISOR_PROMPT = """
            你是“医院业财银企直连系统”总助手。
            问候、能力说明、澄清缺失条件、非业务闲聊：你自己用自然语言回答，不要编造业务数据。
            只读业务（点查、汇总、导出、图表）必须通过 agent_spawn 交给子 Agent，禁止自己编造账户、金额、笔数。
            写操作（提交支付、从银行同步流水/回单/对账单）由你直接调用写工具完成，调用前用一句话说明即将执行的操作；工具会走用户确认。
            agent_spawn 时 timeout_seconds 必须设为 %d（同步等待，以便用户看到子 Agent 流式过程）。
            一次只 spawn 一个子 Agent；有依赖时等上一步结果再 spawn 下一步（例如先汇总再图表）。
            
            子 Agent：
            - query_bank：点查账户/单号/少量明细（最多 10 条）
            - summarize_bank：合计、笔数、占比、分组统计（SQL 聚合）
            - export_excel：导出 Excel
            - create_chart：柱状/折线/饼图
            
            用户说提交支付、付款、同步流水、同步回单、同步对账单、拉银行数据时：直接调用写工具，不要 spawn 子 Agent。
            只查已有数据用 query_bank，不要调用写工具。
            子 Agent 或写工具返回后，用简洁自然语言向用户转述要点，不要输出 JSON 路由数组。
            当前日期：%s
            """;

    /**
     * @param properties 配置
     * @return DashScope 模型
     */
    @Bean
    public Model chatModel(AgentScopeProperties properties) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("pmc.agent.api-key / AI_DASHSCOPE_API_KEY is required");
        }
        return DashScopeChatModel.builder()
                .apiKey(properties.getApiKey())
                .modelName(properties.getModel())
                .stream(true)
                .build();
    }

    /**
     * @param dataSource Spring 数据源（会话状态写入独立库 agentscope）
     * @return MySQL DistributedStore
     */
    @Bean
    public DistributedStore distributedStore(DataSource dataSource) {
        return MysqlDistributedStore.create(dataSource);
    }

    @Bean
    @AguiAgentId("pmc_supervisor")
    public HarnessAgent supervisorAgent(
            AgentScopeProperties properties,
            Model chatModel,
            DistributedStore distributedStore,

            BankQueryTools bankQueryTools,
            BankAggregateTools bankAggregateTools,
            ExcelExportTools excelExportTools,
            ChartTools chartTools,
            BankWriteTools bankWriteTools,
            WritePermissionResumeMiddleware writePermissionResumeMiddleware) {
        int timeout = properties.getSpawnTimeoutSeconds();
        Toolkit toolkit = new Toolkit(ToolkitConfig.builder().parallel(false).build());
        toolkit.registerTool(bankWriteTools);

        HarnessAgent supervisor = HarnessAgent.builder()
                .name("pmc_supervisor")
                .description("医院业财银企直连系统总助手")
                .sysPrompt(SUPERVISOR_PROMPT.formatted(timeout, today()))
                .model(chatModel)
                .toolkit(toolkit)
                .distributedStore(distributedStore)
                .filesystem(new RemoteFilesystemSpec(distributedStore.baseStore()))
                .maxIters(12)
                .compaction(CompactionConfig.builder()
                        .triggerMessages(30)
                        .keepMessages(10)
                        .truncateArgs(CompactionConfig.TruncateArgsConfig.builder()
                                .maxArgLength(2000)
                                .truncationText("... [truncated] ...")
                                .build())
                        .build())
                .toolResultEviction(ToolResultEvictionConfig.defaults())
                .disableFilesystemTools()
                .disableShellTool()
                .disableMemoryTools()
                .disableMemoryHooks()
                .disableDynamicSkills()
                .disableDefaultWorkspaceSkills()
                .middleware(new TimingMiddleware())
                .middleware(writePermissionResumeMiddleware)
                .permissionContext(Permissions.askPermissions(List.of(
                        "submitBankPayOrder",
                        "syncTradeDetails",
                        "syncBalanceFlows",
                        "syncElectronicReceipts",
                        "syncElectronicStatements")))
                .subagent(getSubagentDeclaration("query_bank", """
                        点查银行账户、支付单、支付明细、交易明细、电子回单、余额流水、电子对账单。
                        适用于查某一账户、某一单号、最近几笔明细。最多返回 10 条，不适合汇总或导出。
                        """, QUERY_PROMPT.formatted(today())))
                .subagent(getSubagentDeclaration("summarize_bank", """
                        对交易明细、支付单、支付明细单、账户、电子回单、电子对账单做 SQL 汇总：合计金额、笔数、按账号/日/月/状态等分组。
                        适用于“一共多少”“合计”“占比”“按月统计”。
                        """, SUMMARY_PROMPT.formatted(today())))
                .subagent(getSubagentDeclaration("export_excel", """
                        将账户、交易明细、支付单或汇总结果导出为 Excel，返回下载地址。
                        用户说导出、下载表格、做成 Excel 时使用。
                        """, EXCEL_PROMPT.formatted(today())))
                .subagent(getSubagentDeclaration("create_chart", """
                        根据银企汇总数据生成柱状图、折线图或饼图。
                        用户说图表、趋势、对比、可视化时使用。
                        """, CHART_PROMPT.formatted(today())))
                .subagentFactory("query_bank", name -> getReActAgent(name, QUERY_PROMPT.formatted(today()), chatModel, bankQueryTools))
                .subagentFactory("export_excel", name -> getReActAgent(name, EXCEL_PROMPT.formatted(today()), chatModel, excelExportTools))
                .subagentFactory("create_chart", name -> getReActAgent(name, CHART_PROMPT.formatted(today()), chatModel, chartTools))
                .subagentFactory("summarize_bank", name -> getReActAgent(name, SUMMARY_PROMPT.formatted(today()), chatModel, bankAggregateTools))
                .build();

        log.info("HarnessAgent pmc_supervisor ready, model={}, spawnTimeout={}s", properties.getModel(), timeout);
        return supervisor;
    }

    private static ReActAgent getReActAgent(
            String name,
            String prompt,
            Model chatModel,
            Object tools) {
        Toolkit toolkit = new Toolkit(ToolkitConfig.builder().parallel(false).build());
        toolkit.registerTool(tools);
        return ReActAgent.builder()
                .name(name)
                .sysPrompt(prompt)
                .model(chatModel)
                .toolkit(toolkit)
                .maxIters(8)
                .permissionContext(Permissions.bypassPermissions())
                .build();
    }

    private static SubagentDeclaration getSubagentDeclaration(
            String name, String description, String body) {
        return SubagentDeclaration.builder()
                .name(name)
                .description(description.trim())
                .inlineAgentsBody(body)
                .mode(SubagentDeclaration.Mode.SUBAGENT)
                .steps(8)
                .build();
    }

    private static String today() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);
    }
}
