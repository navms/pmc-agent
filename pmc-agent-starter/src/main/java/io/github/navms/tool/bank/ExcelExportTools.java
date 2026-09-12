package io.github.navms.tool.bank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.navms.application.artifact.ExcelExportAppService;
import io.github.navms.bank.api.dto.AccountQuery;
import io.github.navms.bank.api.dto.BankAggregateQuery;
import io.github.navms.bank.api.dto.BankPayOrderQuery;
import io.github.navms.bank.api.dto.TradeDetailQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Excel 导出 Tool，只返回下载地址，不返回单元格内容。
 *
 * @author navms
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExcelExportTools {

    private final ExcelExportAppService excelExportAppService;

    private final ObjectMapper objectMapper;

    /**
     * @return JSON
     */
    @Tool(description = "导出交易明细为 Excel。按账号、借贷方向、日期过滤。返回 downloadUrl，不要把表格内容复述给用户，请提示下载。最多 20000 行。")
    public String exportTradeDetailsExcel(
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "账号，可选", required = false) String accountNo,
            @ToolParam(description = "借贷方向 debit/credit，可选", required = false) String direction,
            @ToolParam(description = "交易日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "交易日止 yyyy-MM-dd", required = false) String endDate) {
        return toJson(excelExportAppService.exportTradeDetails(
                new TradeDetailQuery(tenantId, accountNo, null, null, direction, startDate, endDate, null)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "导出银行支付单为 Excel。返回 downloadUrl。最多 20000 行。")
    public String exportPayOrdersExcel(
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "支付单号，可选", required = false) String bankPayOrderNo,
            @ToolParam(description = "付款账号，可选", required = false) String accountNo,
            @ToolParam(description = "状态 submitted/success/failed/returned，可选", required = false) String status,
            @ToolParam(description = "申请日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "申请日止 yyyy-MM-dd", required = false) String endDate) {
        return toJson(excelExportAppService.exportPayOrders(
                new BankPayOrderQuery(tenantId, bankPayOrderNo, accountNo, status, startDate, endDate, null)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "导出银行账户列表为 Excel。返回 downloadUrl。")
    public String exportAccountsExcel(
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "账号，可选", required = false) String accountNo,
            @ToolParam(description = "户名关键字，可选", required = false) String accountName,
            @ToolParam(description = "状态 active/frozen/closed，可选", required = false) String status) {
        return toJson(excelExportAppService.exportAccounts(new AccountQuery(tenantId, accountNo, accountName, status, null)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "将汇总结果导出为 Excel。dataset：trade、pay_order、pay_order_detail、account、receipt、statement。返回 downloadUrl。")
    public String exportSummaryExcel(
            @ToolParam(description = "数据集 trade/pay_order/pay_order_detail/account/receipt/statement", required = false) String dataset,
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "账号，可选", required = false) String accountNo,
            @ToolParam(description = "分组 account/day/month/direction/counterpart/status", required = false) String groupBy,
            @ToolParam(description = "日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "日止 yyyy-MM-dd", required = false) String endDate) {
        return toJson(excelExportAppService.exportSummary(
                dataset == null ? "trade" : dataset,
                new BankAggregateQuery(tenantId, accountNo, null, null, startDate, endDate, groupBy)));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize excel result", e);
            return "{\"success\":false,\"message\":\"导出结果序列化失败\"}";
        }
    }
}
