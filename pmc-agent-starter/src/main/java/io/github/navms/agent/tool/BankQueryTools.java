package io.github.navms.agent.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.navms.bank.api.BankQueryApi;
import io.github.navms.bank.api.dto.AccountQuery;
import io.github.navms.bank.api.dto.BalanceFlowQuery;
import io.github.navms.bank.api.dto.BankPayOrderDetailQuery;
import io.github.navms.bank.api.dto.BankPayOrderQuery;
import io.github.navms.bank.api.dto.BankQueryResult;
import io.github.navms.bank.api.dto.ElectronicReceiptQuery;
import io.github.navms.bank.api.dto.ElectronicStatementQuery;
import io.github.navms.bank.api.dto.TradeDetailQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 银企直连查询 Tool，只读，数据来自内存 Mock。
 *
 * @author navms
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankQueryTools {

    private final BankQueryApi bankQueryApi;

    private final ObjectMapper objectMapper;

    /**
     * @param tenantId    租户 ID
     * @param accountNo   账号
     * @param accountName 户名
     * @param status      状态 active/frozen/closed
     * @return JSON
     */
    @Tool(description = "查询银行账户。可按租户、账号、户名、状态过滤。未传租户时默认当前医院租户 T001。返回账号、户名、开户行、币种、账户类型、状态、当前余额。")
    public String queryAccounts(
            @ToolParam(name = "tenantId", description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(name = "accountNo", description = "银行账号，可选", required = false) String accountNo,
            @ToolParam(name = "accountName", description = "户名关键字，可选", required = false) String accountName,
            @ToolParam(name = "status", description = "账户状态：active / frozen / closed，可选", required = false) String status) {
        return toJson(bankQueryApi.queryAccounts(new AccountQuery(tenantId, accountNo, accountName, status, null)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "查询银行支付单 BankPayOrder。可按租户、支付单号 bankPayOrderNo、付款账号、状态、申请日期区间过滤。状态：submitted / success / failed / returned。日期格式 yyyy-MM-dd。")
    public String queryBankPayOrders(
            @ToolParam(name = "tenantId", description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(name = "bankPayOrderNo", description = "支付单号", required = false) String bankPayOrderNo,
            @ToolParam(name = "accountNo", description = "付款账号", required = false) String accountNo,
            @ToolParam(name = "status", description = "状态 submitted/success/failed/returned", required = false) String status,
            @ToolParam(name = "startDate", description = "申请日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(name = "endDate", description = "申请日止 yyyy-MM-dd", required = false) String endDate) {
        return toJson(bankQueryApi.queryBankPayOrders(
                new BankPayOrderQuery(tenantId, bankPayOrderNo, accountNo, status, startDate, endDate, null)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "查询银行支付明细单 BankPayOrderDetail。通过 bankPayOrderNo 或 bankPayOrderDetailNo 关联支付单与交易明细。明细号 bankPayOrderDetailNo 等于交易明细号 tradeDetailNo。")
    public String queryBankPayOrderDetails(
            @ToolParam(name = "tenantId", description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(name = "bankPayOrderNo", description = "支付单号", required = false) String bankPayOrderNo,
            @ToolParam(name = "bankPayOrderDetailNo", description = "支付明细号，等于交易明细号", required = false) String bankPayOrderDetailNo,
            @ToolParam(name = "accountNo", description = "付款账号", required = false) String accountNo,
            @ToolParam(name = "status", description = "状态 submitted/success/failed/returned", required = false) String status) {
        return toJson(bankQueryApi.queryBankPayOrderDetails(
                new BankPayOrderDetailQuery(tenantId, bankPayOrderNo, bankPayOrderDetailNo, accountNo, status, null)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "查询交易明细 TradeDetail。可按账号、交易明细号 tradeDetailNo、回单号 receiptNo、借贷方向 debit/credit、交易日期区间过滤。日期格式 yyyy-MM-dd。")
    public String queryTradeDetails(
            @ToolParam(name = "tenantId", description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(name = "accountNo", description = "账号", required = false) String accountNo,
            @ToolParam(name = "tradeDetailNo", description = "交易明细号", required = false) String tradeDetailNo,
            @ToolParam(name = "receiptNo", description = "电子回单号", required = false) String receiptNo,
            @ToolParam(name = "direction", description = "借贷方向 debit/credit", required = false) String direction,
            @ToolParam(name = "startDate", description = "交易日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(name = "endDate", description = "交易日止 yyyy-MM-dd", required = false) String endDate) {
        return toJson(bankQueryApi.queryTradeDetails(
                new TradeDetailQuery(tenantId, accountNo, tradeDetailNo, receiptNo, direction, startDate, endDate, null)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "查询电子回单 ElectronicReceipt。回单号 receiptNo、交易明细号 tradeDetailNo 与交易明细一致。可按账号、出具日期区间过滤。日期格式 yyyy-MM-dd。")
    public String queryElectronicReceipts(
            @ToolParam(name = "tenantId", description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(name = "receiptNo", description = "回单号", required = false) String receiptNo,
            @ToolParam(name = "tradeDetailNo", description = "交易明细号", required = false) String tradeDetailNo,
            @ToolParam(name = "accountNo", description = "账号", required = false) String accountNo,
            @ToolParam(name = "startDate", description = "出具日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(name = "endDate", description = "出具日止 yyyy-MM-dd", required = false) String endDate) {
        return toJson(bankQueryApi.queryElectronicReceipts(
                new ElectronicReceiptQuery(tenantId, receiptNo, tradeDetailNo, accountNo, startDate, endDate, null)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "查询余额流水 BalanceFlow。可按租户、账号、发生日期区间过滤。日期格式 yyyy-MM-dd。返回变动金额与变动前后余额。")
    public String queryBalanceFlows(
            @ToolParam(name = "tenantId", description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(name = "accountNo", description = "账号", required = false) String accountNo,
            @ToolParam(name = "startDate", description = "发生日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(name = "endDate", description = "发生日止 yyyy-MM-dd", required = false) String endDate) {
        return toJson(bankQueryApi.queryBalanceFlows(
                new BalanceFlowQuery(tenantId, accountNo, startDate, endDate, null)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "查询电子对账单 ElectronicStatement。对账单号 statementNo 与交易明细回单号 receiptNo 相同，tradeDetailNo 与交易明细号相同。可按账号、对账周期过滤。日期格式 yyyy-MM-dd。")
    public String queryElectronicStatements(
            @ToolParam(name = "tenantId", description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(name = "statementNo", description = "对账单号，等于回单号", required = false) String statementNo,
            @ToolParam(name = "tradeDetailNo", description = "交易明细号", required = false) String tradeDetailNo,
            @ToolParam(name = "accountNo", description = "账号", required = false) String accountNo,
            @ToolParam(name = "periodStart", description = "对账周期起 yyyy-MM-dd", required = false) String periodStart,
            @ToolParam(name = "periodEnd", description = "对账周期止 yyyy-MM-dd", required = false) String periodEnd) {
        return toJson(bankQueryApi.queryElectronicStatements(
                new ElectronicStatementQuery(tenantId, statementNo, tradeDetailNo, accountNo, periodStart, periodEnd, null)));
    }

    private String toJson(BankQueryResult<?> result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize bank query result", e);
            return "{\"success\":false,\"message\":\"查询结果序列化失败\",\"data\":[]}";
        }
    }
}
