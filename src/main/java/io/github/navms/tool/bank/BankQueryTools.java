package io.github.navms.tool.bank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.navms.application.bank.dto.AccountQuery;
import io.github.navms.application.bank.dto.BalanceFlowQuery;
import io.github.navms.application.bank.dto.BankPayOrderDetailQuery;
import io.github.navms.application.bank.dto.BankPayOrderQuery;
import io.github.navms.application.bank.dto.BankQueryResult;
import io.github.navms.application.bank.dto.ElectronicReceiptQuery;
import io.github.navms.application.bank.dto.ElectronicStatementQuery;
import io.github.navms.application.bank.dto.TradeDetailQuery;
import io.github.navms.application.bank.service.BankQueryAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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

    private final BankQueryAppService bankQueryAppService;

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
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "银行账号，可选", required = false) String accountNo,
            @ToolParam(description = "户名关键字，可选", required = false) String accountName,
            @ToolParam(description = "账户状态：active / frozen / closed，可选", required = false) String status) {
        return toJson(bankQueryAppService.queryAccounts(new AccountQuery(tenantId, accountNo, accountName, status)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "查询银行支付单 BankPayOrder。可按租户、支付单号 bankPayOrderNo、付款账号、状态、申请日期区间过滤。状态：submitted / success / failed / returned。日期格式 yyyy-MM-dd。")
    public String queryBankPayOrders(
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "支付单号", required = false) String bankPayOrderNo,
            @ToolParam(description = "付款账号", required = false) String accountNo,
            @ToolParam(description = "状态 submitted/success/failed/returned", required = false) String status,
            @ToolParam(description = "申请日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "申请日止 yyyy-MM-dd", required = false) String endDate) {
        return toJson(bankQueryAppService.queryBankPayOrders(
                new BankPayOrderQuery(tenantId, bankPayOrderNo, accountNo, status, startDate, endDate)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "查询银行支付明细单 BankPayOrderDetail。通过 bankPayOrderNo 或 bankPayOrderDetailNo 关联支付单与交易明细。明细号 bankPayOrderDetailNo 等于交易明细号 tradeDetailNo。")
    public String queryBankPayOrderDetails(
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "支付单号", required = false) String bankPayOrderNo,
            @ToolParam(description = "支付明细号，等于交易明细号", required = false) String bankPayOrderDetailNo,
            @ToolParam(description = "付款账号", required = false) String accountNo,
            @ToolParam(description = "状态 submitted/success/failed/returned", required = false) String status) {
        return toJson(bankQueryAppService.queryBankPayOrderDetails(
                new BankPayOrderDetailQuery(tenantId, bankPayOrderNo, bankPayOrderDetailNo, accountNo, status)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "查询交易明细 TradeDetail。可按账号、交易明细号 tradeDetailNo、回单号 receiptNo、借贷方向 debit/credit、交易日期区间过滤。日期格式 yyyy-MM-dd。")
    public String queryTradeDetails(
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "账号", required = false) String accountNo,
            @ToolParam(description = "交易明细号", required = false) String tradeDetailNo,
            @ToolParam(description = "电子回单号", required = false) String receiptNo,
            @ToolParam(description = "借贷方向 debit/credit", required = false) String direction,
            @ToolParam(description = "交易日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "交易日止 yyyy-MM-dd", required = false) String endDate) {
        return toJson(bankQueryAppService.queryTradeDetails(
                new TradeDetailQuery(tenantId, accountNo, tradeDetailNo, receiptNo, direction, startDate, endDate)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "查询电子回单 ElectronicReceipt。回单号 receiptNo、交易明细号 tradeDetailNo 与交易明细一致。可按账号、出具日期区间过滤。日期格式 yyyy-MM-dd。")
    public String queryElectronicReceipts(
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "回单号", required = false) String receiptNo,
            @ToolParam(description = "交易明细号", required = false) String tradeDetailNo,
            @ToolParam(description = "账号", required = false) String accountNo,
            @ToolParam(description = "出具日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "出具日止 yyyy-MM-dd", required = false) String endDate) {
        return toJson(bankQueryAppService.queryElectronicReceipts(
                new ElectronicReceiptQuery(tenantId, receiptNo, tradeDetailNo, accountNo, startDate, endDate)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "查询余额流水 BalanceFlow。可按租户、账号、发生日期区间过滤。日期格式 yyyy-MM-dd。返回变动金额与变动前后余额。")
    public String queryBalanceFlows(
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "账号", required = false) String accountNo,
            @ToolParam(description = "发生日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "发生日止 yyyy-MM-dd", required = false) String endDate) {
        return toJson(bankQueryAppService.queryBalanceFlows(
                new BalanceFlowQuery(tenantId, accountNo, startDate, endDate)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "查询电子对账单 ElectronicStatement。对账单号 statementNo 与交易明细回单号 receiptNo 相同，tradeDetailNo 与交易明细号相同。可按账号、对账周期过滤。日期格式 yyyy-MM-dd。")
    public String queryElectronicStatements(
            @ToolParam(description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(description = "对账单号，等于回单号", required = false) String statementNo,
            @ToolParam(description = "交易明细号", required = false) String tradeDetailNo,
            @ToolParam(description = "账号", required = false) String accountNo,
            @ToolParam(description = "对账周期起 yyyy-MM-dd", required = false) String periodStart,
            @ToolParam(description = "对账周期止 yyyy-MM-dd", required = false) String periodEnd) {
        return toJson(bankQueryAppService.queryElectronicStatements(
                new ElectronicStatementQuery(tenantId, statementNo, tradeDetailNo, accountNo, periodStart, periodEnd)));
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
