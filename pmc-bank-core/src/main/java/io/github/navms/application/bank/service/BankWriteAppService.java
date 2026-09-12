package io.github.navms.application.bank.service;

import io.github.navms.application.bank.support.BankQuerySupport;
import io.github.navms.application.bank.support.BankQuerySupport.DateRange;
import io.github.navms.bank.api.BankWriteApi;
import io.github.navms.bank.api.dto.BankPayOrderSubmitInfo;
import io.github.navms.bank.api.dto.BankSyncInfo;
import io.github.navms.bank.api.dto.BankWriteResult;
import io.github.navms.bank.api.dto.SubmitBankPayOrderCommand;
import io.github.navms.bank.api.dto.SyncBankDataCommand;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 银企写操作用例。当前为 mock，不落库、不调真实银行。
 *
 * @author navms
 */
@Service
public class BankWriteAppService implements BankWriteApi {

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    /**
     * {@inheritDoc}
     */
    @Override
    public BankWriteResult<BankPayOrderSubmitInfo> submitBankPayOrder(SubmitBankPayOrderCommand command) {
        if (command == null || !StringUtils.hasText(command.accountNo())) {
            return BankWriteResult.fail("付款账号不能为空");
        }
        if (command.totalAmount() == null || command.totalAmount().signum() <= 0) {
            return BankWriteResult.fail("合计金额必须大于 0");
        }
        String tenantId = BankQuerySupport.resolveTenant(command.tenantId());
        String accountNo = command.accountNo().trim();
        String orderNo = StringUtils.hasText(command.bankPayOrderNo())
                ? command.bankPayOrderNo().trim()
                : "PO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format(Locale.ROOT, "%04d", ThreadLocalRandom.current().nextInt(10_000));
        int totalCount = command.totalCount() == null || command.totalCount() <= 0 ? 1 : command.totalCount();
        String purpose = BankQuerySupport.blankToNull(command.purpose());
        String summary = BankQuerySupport.blankToNull(command.summary());
        String acceptNo = "MOCK-BA-" + LocalDate.now().format(DAY) + "-" + Math.abs(orderNo.hashCode() % 1_000_000);
        BankPayOrderSubmitInfo info = new BankPayOrderSubmitInfo(
                true,
                tenantId,
                orderNo,
                accountNo,
                command.totalAmount(),
                totalCount,
                purpose,
                summary,
                "submitted",
                acceptNo);
        return BankWriteResult.ok(info, "已 mock 提交银行支付单，未调用真实银行接口");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BankWriteResult<BankSyncInfo> syncTradeDetails(SyncBankDataCommand command) {
        return mockSync(command, "trade_detail", "交易明细");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BankWriteResult<BankSyncInfo> syncBalanceFlows(SyncBankDataCommand command) {
        return mockSync(command, "balance_flow", "余额流水");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BankWriteResult<BankSyncInfo> syncElectronicReceipts(SyncBankDataCommand command) {
        return mockSync(command, "electronic_receipt", "电子回单");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BankWriteResult<BankSyncInfo> syncElectronicStatements(SyncBankDataCommand command) {
        return mockSync(command, "electronic_statement", "电子对账单");
    }

    private BankWriteResult<BankSyncInfo> mockSync(SyncBankDataCommand command, String dataType, String label) {
        if (command == null || !StringUtils.hasText(command.accountNo())) {
            return BankWriteResult.fail("账号不能为空");
        }
        DateRange dates = BankQuerySupport.parseRange(command.startDate(), command.endDate());
        if (dates.error() != null) {
            return BankWriteResult.fail(dates.error());
        }
        String tenantId = BankQuerySupport.resolveTenant(command.tenantId());
        String accountNo = command.accountNo().trim();
        String start = dates.start() == null ? null : dates.start().toString();
        String end = dates.end() == null ? null : dates.end().toString();
        int syncedCount = mockSyncedCount(accountNo, dataType, start, end);
        BankSyncInfo info = new BankSyncInfo(true, tenantId, accountNo, dataType, start, end, syncedCount);
        return BankWriteResult.ok(info, "已 mock 从银行同步" + label + " " + syncedCount + " 笔，未落库");
    }

    private static int mockSyncedCount(String accountNo, String dataType, String start, String end) {
        int seed = (accountNo + dataType + start + end).hashCode();
        return 3 + Math.floorMod(seed, 12);
    }
}
