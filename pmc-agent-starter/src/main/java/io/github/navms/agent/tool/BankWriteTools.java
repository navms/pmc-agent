package io.github.navms.agent.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.github.navms.bank.api.BankWriteApi;
import io.github.navms.bank.api.dto.BankWriteResult;
import io.github.navms.bank.api.dto.SubmitBankPayOrderCommand;
import io.github.navms.bank.api.dto.SyncBankDataCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 银企直连写操作 Tool：支付提交与从银行拉数同步。
 *
 * @author navms
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankWriteTools {

    private final BankWriteApi bankWriteApi;

    private final ObjectMapper objectMapper;

    /**
     * @return JSON
     */
    @Tool(description = "向银行提交支付单。会调用银行接口发起付款，执行前必须经用户确认。accountNo 与 totalAmount 必填。")
    public String submitBankPayOrder(
            @ToolParam(name = "tenantId", description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(name = "accountNo", description = "付款账号") String accountNo,
            @ToolParam(name = "totalAmount", description = "合计金额，必须大于 0") String totalAmount,
            @ToolParam(name = "bankPayOrderNo", description = "支付单号，可选，空则生成", required = false) String bankPayOrderNo,
            @ToolParam(name = "totalCount", description = "明细笔数，可选", required = false) Integer totalCount,
            @ToolParam(name = "purpose", description = "用途，可选", required = false) String purpose,
            @ToolParam(name = "summary", description = "摘要，可选", required = false) String summary) {
        BigDecimal amount = parseAmount(totalAmount);
        if (amount == null) {
            return toJson(BankWriteResult.fail("合计金额格式无效，须为大于 0 的数字"));
        }
        return toJson(bankWriteApi.submitBankPayOrder(
                new SubmitBankPayOrderCommand(tenantId, bankPayOrderNo, accountNo, amount, totalCount, purpose, summary)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "从银行同步交易明细到本系统。会调用银行查询接口拉数入库，执行前必须经用户确认。账号必填，日期 yyyy-MM-dd。")
    public String syncTradeDetails(
            @ToolParam(name = "tenantId", description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(name = "accountNo", description = "账号") String accountNo,
            @ToolParam(name = "startDate", description = "交易日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(name = "endDate", description = "交易日止 yyyy-MM-dd", required = false) String endDate) {
        return toJson(bankWriteApi.syncTradeDetails(new SyncBankDataCommand(tenantId, accountNo, startDate, endDate)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "从银行同步余额流水到本系统。会调用银行查询接口拉数入库，执行前必须经用户确认。账号必填，日期 yyyy-MM-dd。")
    public String syncBalanceFlows(
            @ToolParam(name = "tenantId", description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(name = "accountNo", description = "账号") String accountNo,
            @ToolParam(name = "startDate", description = "发生日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(name = "endDate", description = "发生日止 yyyy-MM-dd", required = false) String endDate) {
        return toJson(bankWriteApi.syncBalanceFlows(new SyncBankDataCommand(tenantId, accountNo, startDate, endDate)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "从银行同步电子回单到本系统。会调用银行查询接口拉数入库，执行前必须经用户确认。账号必填，日期 yyyy-MM-dd。")
    public String syncElectronicReceipts(
            @ToolParam(name = "tenantId", description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(name = "accountNo", description = "账号") String accountNo,
            @ToolParam(name = "startDate", description = "出具日起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(name = "endDate", description = "出具日止 yyyy-MM-dd", required = false) String endDate) {
        return toJson(bankWriteApi.syncElectronicReceipts(new SyncBankDataCommand(tenantId, accountNo, startDate, endDate)));
    }

    /**
     * @return JSON
     */
    @Tool(description = "从银行同步电子对账单到本系统。会调用银行查询接口拉数入库，执行前必须经用户确认。账号必填，日期 yyyy-MM-dd。")
    public String syncElectronicStatements(
            @ToolParam(name = "tenantId", description = "租户ID，可选，默认 T001", required = false) String tenantId,
            @ToolParam(name = "accountNo", description = "账号") String accountNo,
            @ToolParam(name = "startDate", description = "对账周期起 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(name = "endDate", description = "对账周期止 yyyy-MM-dd", required = false) String endDate) {
        return toJson(bankWriteApi.syncElectronicStatements(new SyncBankDataCommand(tenantId, accountNo, startDate, endDate)));
    }

    private static BigDecimal parseAmount(String totalAmount) {
        if (!StringUtils.hasText(totalAmount)) {
            return null;
        }
        try {
            return new BigDecimal(totalAmount.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String toJson(BankWriteResult<?> result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize bank write result", e);
            return "{\"success\":false,\"message\":\"写操作结果序列化失败\",\"data\":null}";
        }
    }
}
