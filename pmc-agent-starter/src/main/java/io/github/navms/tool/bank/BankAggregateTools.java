package io.github.navms.tool.bank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.navms.bank.api.BankAggregateApi;
import io.github.navms.bank.api.dto.BankAggregateQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 银企汇总 Tool，返回 SQL 聚合结果，不返回明细全量。
 *
 * @author navms
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankAggregateTools {

    private final BankAggregateApi bankAggregateApi;

    private final ObjectMapper objectMapper;

    /**
     * @return JSON
     */
    @Tool(description = "汇总交易明细。按账号/日/月/借贷方向/对手方分组，返回笔数与金额合计。groupBy：account、day、month、direction、counterpart。日期 yyyy-MM-dd。不要用本工具查单笔明细。")
    public String summarizeTradeDetails(
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "账号，可选", required = false) String accountNo,
            @ToolParam(description = "借贷方向 debit/credit，可选", required = false) String direction,
            @ToolParam(description = "交易日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "交易日止 yyyy-MM-dd", required = false) String endDate,
            @ToolParam(description = "分组 account/day/month/direction/counterpart，默认 account", required = false) String groupBy) {
        return toJson(bankAggregateApi.summarizeTradeDetails(
                new BankAggregateQuery(tenantId, accountNo, direction, null, startDate, endDate, groupBy)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "汇总银行支付单。按账号/日/月/状态分组，返回笔数与合计金额。groupBy：account、day、month、status。日期 yyyy-MM-dd。")
    public String summarizePayOrders(
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "付款账号，可选", required = false) String accountNo,
            @ToolParam(description = "状态 submitted/success/failed/returned，可选", required = false) String status,
            @ToolParam(description = "申请日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "申请日止 yyyy-MM-dd", required = false) String endDate,
            @ToolParam(description = "分组 account/day/month/status，默认 status", required = false) String groupBy) {
        return toJson(bankAggregateApi.summarizePayOrders(
                new BankAggregateQuery(tenantId, accountNo, null, status, startDate, endDate, groupBy)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "汇总银行支付明细单。按账号/状态/收款人/日/月分组，返回笔数与金额合计。groupBy：account、status、counterpart、day、month。日期按创建日 yyyy-MM-dd。")
    public String summarizePayOrderDetails(
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "付款账号，可选", required = false) String accountNo,
            @ToolParam(description = "状态 submitted/success/failed/returned，可选", required = false) String status,
            @ToolParam(description = "创建日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "创建日止 yyyy-MM-dd", required = false) String endDate,
            @ToolParam(description = "分组 account/status/counterpart/day/month，默认 status", required = false) String groupBy) {
        return toJson(bankAggregateApi.summarizePayOrderDetails(
                new BankAggregateQuery(tenantId, accountNo, null, status, startDate, endDate, groupBy)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "汇总银行账户。按状态/类型/开户行/账号分组，返回户数与余额合计。groupBy：status、type、bank、account。不要用本工具查单户明细。")
    public String summarizeAccounts(
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "账号，可选", required = false) String accountNo,
            @ToolParam(description = "状态 active/frozen/closed，可选", required = false) String status,
            @ToolParam(description = "分组 status/type/bank/account，默认 status", required = false) String groupBy) {
        return toJson(bankAggregateApi.summarizeAccounts(
                new BankAggregateQuery(tenantId, accountNo, null, status, null, null, groupBy)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "汇总电子回单。按账号/日/月分组，返回张数与金额合计。groupBy：account、day、month。日期为出具日 yyyy-MM-dd。")
    public String summarizeElectronicReceipts(
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "账号，可选", required = false) String accountNo,
            @ToolParam(description = "出具日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "出具日止 yyyy-MM-dd", required = false) String endDate,
            @ToolParam(description = "分组 account/day/month，默认 account", required = false) String groupBy) {
        return toJson(bankAggregateApi.summarizeReceipts(
                new BankAggregateQuery(tenantId, accountNo, null, null, startDate, endDate, groupBy)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "汇总电子对账单。按账号/状态/日/月分组，返回份数与借贷发生额合计。groupBy：account、status、day、month。日期为出具日 yyyy-MM-dd。")
    public String summarizeElectronicStatements(
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "账号，可选", required = false) String accountNo,
            @ToolParam(description = "状态 issued/confirmed，可选", required = false) String status,
            @ToolParam(description = "出具日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "出具日止 yyyy-MM-dd", required = false) String endDate,
            @ToolParam(description = "分组 account/status/day/month，默认 account", required = false) String groupBy) {
        return toJson(bankAggregateApi.summarizeStatements(
                new BankAggregateQuery(tenantId, accountNo, null, status, startDate, endDate, groupBy)));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize aggregate result", e);
            return "{\"success\":false,\"message\":\"汇总结果序列化失败\",\"data\":[]}";
        }
    }
}
