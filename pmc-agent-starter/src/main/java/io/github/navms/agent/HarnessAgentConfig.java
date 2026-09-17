package io.github.navms.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolkitConfig;
import io.agentscope.extensions.jdbc.JdbcDistributedStore;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.harness.agent.DistributedStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.filesystem.spec.RemoteFilesystemSpec;
import io.agentscope.harness.agent.memory.MemoryConfig;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import io.agentscope.harness.agent.memory.compaction.ToolResultEvictionConfig;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import io.agentscope.spring.boot.agui.common.AguiAgentId;
import io.github.navms.agent.hitl.WritePermissionResumeMiddleware;
import io.github.navms.agent.middle.LangfuseSessionMiddleware;
import io.github.navms.agent.permission.Permissions;
import io.github.navms.agent.prompt.LangfusePromptRegistry;
import io.github.navms.agent.prompt.LangfusePromptService;
import io.github.navms.agent.prompt.Prompt;
import io.github.navms.agent.tool.BankAggregateTools;
import io.github.navms.agent.tool.BankQueryTools;
import io.github.navms.agent.tool.BankWriteTools;
import io.github.navms.agent.tool.ChartTools;
import io.github.navms.agent.tool.ExcelExportTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
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

    /**
     * @param properties 配置
     * @return DashScope 模型
     */
    @Bean
    public Model chatModel(AgentScopeProperties properties) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("pmc.agent.api-key / AI_DASHSCOPE_API_KEY is required");
        }
        return DashScopeChatModel.builder().apiKey(properties.getApiKey()).modelName(properties.getModel()).stream(true).build();
    }

    /**
     * @param dataSource Spring 数据源
     * @return MySQL DistributedStore
     */
    @Bean
    @DependsOn("langfuseOpenTelemetry")
    public DistributedStore distributedStore(DataSource dataSource) {
        return JdbcDistributedStore.create(dataSource);
    }

    /**
     * @param promptRegistry prompt 版本登记，供 span 绑定
     * @return 追踪中间件
     */
    @Bean
    public LangfuseSessionMiddleware langfuseSessionMiddleware(LangfusePromptRegistry promptRegistry) {
        return new LangfuseSessionMiddleware(promptRegistry);
    }

    @Bean
    @AguiAgentId("pmc_supervisor")
    public HarnessAgent supervisorAgent(
            Model chatModel,
            DistributedStore distributedStore,
            LangfusePromptService promptService,
            LangfuseSessionMiddleware langfuseSessionMiddleware,

            BankQueryTools bankQueryTools,
            BankAggregateTools bankAggregateTools,
            ExcelExportTools excelExportTools,
            ChartTools chartTools,
            BankWriteTools bankWriteTools,
            WritePermissionResumeMiddleware writePermissionResumeMiddleware) {
        Toolkit toolkit = new Toolkit(ToolkitConfig.builder().parallel(false).build());
        toolkit.registerTool(bankWriteTools);

        HarnessAgent supervisor = HarnessAgent.builder()
                .name("pmc_supervisor")
                .description("医院业财银企直连系统总助手")
                .sysPrompt(promptService.compile(Prompt.SUPERVISOR))
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
                .memory(MemoryConfig.builder().build())
                .toolResultEviction(ToolResultEvictionConfig.defaults())
                .middleware(langfuseSessionMiddleware)
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
                        """, promptService.compile(Prompt.QUERY_BANK)))
                .subagent(getSubagentDeclaration("summarize_bank", """
                        对交易明细、支付单、支付明细单、账户、电子回单、电子对账单做 SQL 汇总：合计金额、笔数、按账号/日/月/状态等分组。
                        适用于“一共多少”“合计”“占比”“按月统计”。
                        """, promptService.compile(Prompt.SUMMARIZE_BANK)))
                .subagent(getSubagentDeclaration("export_excel", """
                        将账户、交易明细、支付单或汇总结果导出为 Excel，返回下载地址。
                        用户说导出、下载表格、做成 Excel 时使用。
                        """, promptService.compile(Prompt.EXPORT_EXCEL)))
                .subagent(getSubagentDeclaration("create_chart", """
                        根据银企汇总数据生成柱状图、折线图或饼图。
                        用户说图表、趋势、对比、可视化时使用。
                        """, promptService.compile(Prompt.CREATE_CHART)))
                .subagentFactory("query_bank", name -> getReActAgent(name, promptService, Prompt.QUERY_BANK, chatModel, bankQueryTools, langfuseSessionMiddleware))
                .subagentFactory("export_excel", name -> getReActAgent(name, promptService, Prompt.EXPORT_EXCEL, chatModel, excelExportTools, langfuseSessionMiddleware))
                .subagentFactory("create_chart", name -> getReActAgent(name, promptService, Prompt.CREATE_CHART, chatModel, chartTools, langfuseSessionMiddleware))
                .subagentFactory("summarize_bank", name -> getReActAgent(name, promptService, Prompt.SUMMARIZE_BANK, chatModel, bankAggregateTools, langfuseSessionMiddleware))
                .build();

        log.info("HarnessAgent pmc_supervisor ready, model={}", chatModel.getModelName());
        return supervisor;
    }

    private static ReActAgent getReActAgent(
            String name,
            LangfusePromptService promptService,
            Prompt prompt,
            Model chatModel,
            Object tools,
            LangfuseSessionMiddleware langfuseSessionMiddleware) {
        Toolkit toolkit = new Toolkit(ToolkitConfig.builder().parallel(false).build());
        toolkit.registerTool(tools);
        return ReActAgent.builder()
                .name(name)
                .sysPrompt(promptService.compile(prompt))
                .model(chatModel)
                .toolkit(toolkit)
                .maxIters(8)
                .permissionContext(Permissions.bypassPermissions())
                .middleware(langfuseSessionMiddleware)
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

}
