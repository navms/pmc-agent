package io.github.navms.tool.bank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.navms.application.chart.ChartAppService;
import io.github.navms.application.chart.dto.ChartCommand;
import io.github.navms.bank.api.dto.BankAggregateQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 图表 Tool，返回 ECharts option，不返回明细全量。
 *
 * @author navms
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChartTools {

    private final ChartAppService chartAppService;

    private final ObjectMapper objectMapper;

    /**
     * @return JSON
     */
    @Tool(description = "根据银企汇总数据生成图表。dataset：trade、pay_order、pay_order_detail、account、receipt、statement。chartType：bar/line/pie。groupBy 与对应汇总工具相同。返回 option 供前端渲染，不要编造数据点。")
    public String createBankChart(
            @ToolParam(name = "dataset", description = "数据集 trade/pay_order/pay_order_detail/account/receipt/statement，默认 trade", required = false) String dataset,
            @ToolParam(name = "chartType", description = "图表类型 bar/line/pie，默认 bar", required = false) String chartType,
            @ToolParam(name = "title", description = "图表标题", required = false) String title,
            @ToolParam(name = "tenantId", description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(name = "accountNo", description = "账号，可选", required = false) String accountNo,
            @ToolParam(name = "direction", description = "借贷方向 debit/credit，仅 trade", required = false) String direction,
            @ToolParam(name = "status", description = "支付单状态，仅 pay_order", required = false) String status,
            @ToolParam(name = "groupBy", description = "分组 account/day/month/direction/counterpart/status", required = false) String groupBy,
            @ToolParam(name = "startDate", description = "日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(name = "endDate", description = "日止 yyyy-MM-dd", required = false) String endDate) {
        return toJson(chartAppService.createChart(new ChartCommand(
                dataset,
                chartType,
                title,
                new BankAggregateQuery(tenantId, accountNo, direction, status, startDate, endDate, groupBy))));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chart result", e);
            return "{\"success\":false,\"message\":\"图表结果序列化失败\"}";
        }
    }
}
