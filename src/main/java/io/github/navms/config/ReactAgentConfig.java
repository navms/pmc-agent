package io.github.navms.config;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.mysql.CreateOption;
import com.alibaba.cloud.ai.graph.checkpoint.savers.mysql.MysqlSaver;
import io.github.navms.config.agent.TracingAgentToolCallback;
import io.github.navms.tool.bank.BankAggregateTools;
import io.github.navms.tool.bank.BankQueryTools;
import io.github.navms.tool.bank.ChartTools;
import io.github.navms.tool.bank.ExcelExportTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Supervisor 多 Agent：查询 / 汇总 / Excel / 图表。
 *
 * @author navms
 */
@Slf4j
@Configuration
public class ReactAgentConfig {

    private static final String QUERY_PROMPT = """
            你是医院业财银企直连系统的明细查询助手。
            只处理点查：账户、支付单号、交易明细号、回单号、最近少量流水等。
            必须通过工具获取数据，严禁编造。未传租户时默认 T001。
            工具最多返回 10 条明细，不要用本 Agent 做合计、趋势、占比或导出。
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
            你是“医院业财银企直连系统”的总助手。根据用户意图调用子 Agent 工具，可按顺序调用多个。
            - query_bank：点查账户/单号/少量明细（最多 10 条），不要用它做汇总或导出。
            - summarize_bank：合计、笔数、占比、分组统计（SQL 聚合）。
            - export_excel：导出 Excel，直接调用，不要先 query_bank。
            - create_chart：柱状/折线/饼图，基于汇总而不是 10 条样本。
            所有业务数据必须来自子 Agent 工具结果，严禁编造。
            多步任务（例如先汇总再出图再导出）依次调用对应工具，最后用工具返回的摘要、downloadUrl、图表结果回复用户。
            保持礼貌。当前日期：%s
            """;

    /**
     * @param dataSource 数据源
     * @return 会话检查点
     */
    @Bean
    public MysqlSaver mysqlSaver(DataSource dataSource) {
        return MysqlSaver.builder()
                .dataSource(dataSource)
                .createOption(CreateOption.CREATE_IF_NOT_EXISTS)
                .build();
    }

    /**
     * @param chatModel      模型
     * @param bankQueryTools 明细工具
     * @return 查询 Agent
     */
    @Bean
    public ReactAgent queryAgent(ChatModel chatModel, BankQueryTools bankQueryTools) {
        return ReactAgent.builder()
                .name("query_bank")
                .description("""
                        点查银行账户、支付单、支付明细、交易明细、电子回单、余额流水、电子对账单。
                        适用于查某一账户、某一单号、最近几笔明细。最多返回 10 条，不适合汇总或导出。
                        """)
                .systemPrompt(QUERY_PROMPT.formatted(today()))
                .model(chatModel)
                .methodTools(bankQueryTools)
                .inputType(String.class)
                .build();
    }

    /**
     * @param chatModel          模型
     * @param bankAggregateTools 汇总工具
     * @return 汇总 Agent
     */
    @Bean
    public ReactAgent summaryAgent(ChatModel chatModel, BankAggregateTools bankAggregateTools) {
        return ReactAgent.builder()
                .name("summarize_bank")
                .description("""
                        对交易明细、支付单、支付明细单、账户、电子回单、电子对账单做 SQL 汇总：合计金额、笔数、按账号/日/月/状态等分组。
                        适用于“一共多少”“合计”“占比”“按月统计”。
                        """)
                .systemPrompt(SUMMARY_PROMPT.formatted(today()))
                .model(chatModel)
                .methodTools(bankAggregateTools)
                .inputType(String.class)
                .build();
    }

    /**
     * @param chatModel        模型
     * @param excelExportTools 导出工具
     * @return Excel Agent
     */
    @Bean
    public ReactAgent excelAgent(ChatModel chatModel, ExcelExportTools excelExportTools) {
        return ReactAgent.builder()
                .name("export_excel")
                .description("""
                        将账户、交易明细、支付单或汇总结果导出为 Excel，返回下载地址。
                        用户说导出、下载表格、做成 Excel 时使用。
                        """)
                .systemPrompt(EXCEL_PROMPT.formatted(today()))
                .model(chatModel)
                .methodTools(excelExportTools)
                .inputType(String.class)
                .build();
    }

    /**
     * @param chatModel  模型
     * @param chartTools 图表工具
     * @return 图表 Agent
     */
    @Bean
    public ReactAgent chartAgent(ChatModel chatModel, ChartTools chartTools) {
        return ReactAgent.builder()
                .name("create_chart")
                .description("""
                        根据银企汇总数据生成柱状图、折线图或饼图。
                        用户说图表、趋势、对比、可视化时使用。
                        """)
                .systemPrompt(CHART_PROMPT.formatted(today()))
                .model(chatModel)
                .methodTools(chartTools)
                .inputType(String.class)
                .build();
    }

    /**
     * @param chatModel    模型
     * @param mysqlSaver   检查点
     * @param queryAgent   查询
     * @param summaryAgent 汇总
     * @param excelAgent   Excel
     * @param chartAgent   图表
     * @return 主 Agent
     */
    @Bean
    public ReactAgent supervisorAgent(
            ChatModel chatModel,
            MysqlSaver mysqlSaver,
            @Qualifier("queryAgent") ReactAgent queryAgent,
            @Qualifier("summaryAgent") ReactAgent summaryAgent,
            @Qualifier("excelAgent") ReactAgent excelAgent,
            @Qualifier("chartAgent") ReactAgent chartAgent) {
        ReactAgent supervisorAgent = ReactAgent.builder()
                .name("pmc_supervisor")
                .systemPrompt(SUPERVISOR_PROMPT.formatted(today()))
                .model(chatModel)
                .saver(mysqlSaver)
                .tools(
                        TracingAgentToolCallback.from(queryAgent),
                        TracingAgentToolCallback.from(summaryAgent),
                        TracingAgentToolCallback.from(excelAgent),
                        TracingAgentToolCallback.from(chartAgent))
                .build();

        GraphRepresentation representation = supervisorAgent.getAndCompileGraph()
                .stateGraph
                .getGraph(GraphRepresentation.Type.MERMAID);
        log.info("""
                === Supervisor Graph Structure ===
                {}
                ==================================
                """, representation.content());
        return supervisorAgent;
    }

    private static String today() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);
    }
}
