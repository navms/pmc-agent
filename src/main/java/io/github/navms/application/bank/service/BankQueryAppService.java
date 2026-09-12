package io.github.navms.application.bank.service;

import io.github.navms.application.bank.converter.BankAppConverter;
import io.github.navms.application.bank.dto.AccountInfo;
import io.github.navms.application.bank.dto.AccountQuery;
import io.github.navms.application.bank.dto.BalanceFlowInfo;
import io.github.navms.application.bank.dto.BalanceFlowQuery;
import io.github.navms.application.bank.dto.BankPayOrderDetailInfo;
import io.github.navms.application.bank.dto.BankPayOrderDetailQuery;
import io.github.navms.application.bank.dto.BankPayOrderInfo;
import io.github.navms.application.bank.dto.BankPayOrderQuery;
import io.github.navms.application.bank.dto.BankQueryResult;
import io.github.navms.application.bank.dto.ElectronicReceiptInfo;
import io.github.navms.application.bank.dto.ElectronicReceiptQuery;
import io.github.navms.application.bank.dto.ElectronicStatementInfo;
import io.github.navms.application.bank.dto.ElectronicStatementQuery;
import io.github.navms.application.bank.dto.TradeDetailInfo;
import io.github.navms.application.bank.dto.TradeDetailQuery;
import io.github.navms.application.bank.support.BankQuerySupport;
import io.github.navms.application.bank.support.BankQuerySupport.DateRange;
import io.github.navms.domain.bank.BankDefaults;
import io.github.navms.domain.bank.enums.AccountStatus;
import io.github.navms.domain.bank.enums.PayOrderStatus;
import io.github.navms.domain.bank.enums.TradeDirection;
import io.github.navms.domain.bank.repository.AccountRepository;
import io.github.navms.domain.bank.repository.BalanceFlowRepository;
import io.github.navms.domain.bank.repository.BankPayOrderDetailRepository;
import io.github.navms.domain.bank.repository.BankPayOrderRepository;
import io.github.navms.domain.bank.repository.ElectronicReceiptRepository;
import io.github.navms.domain.bank.repository.ElectronicStatementRepository;
import io.github.navms.domain.bank.repository.TradeDetailRepository;
import io.github.navms.utils.enums.BaseEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 银企直连查询用例。
 *
 * @author navms
 */
@Service
@RequiredArgsConstructor
public class BankQueryAppService {

    private final AccountRepository accountRepository;

    private final BankPayOrderRepository bankPayOrderRepository;

    private final BankPayOrderDetailRepository bankPayOrderDetailRepository;

    private final TradeDetailRepository tradeDetailRepository;

    private final ElectronicReceiptRepository electronicReceiptRepository;

    private final BalanceFlowRepository balanceFlowRepository;

    private final ElectronicStatementRepository electronicStatementRepository;

    /**
     * @param query 查询
     * @return 账户
     */
    public BankQueryResult<AccountInfo> queryAccounts(AccountQuery query) {
        String tenantId = BankQuerySupport.resolveTenant(query.tenantId());
        List<AccountInfo> rows = BankAppConverter.INSTANCE.toAccountInfoList(
                accountRepository.query(
                        tenantId,
                        BankQuerySupport.blankToNull(query.accountNo()),
                        BankQuerySupport.blankToNull(query.accountName()),
                        BaseEnum.of(AccountStatus.class, query.status()),
                        BankDefaults.MAX_QUERY_ROWS));
        return BankQueryResult.ok(rows);
    }

    /**
     * @param query 查询
     * @return 支付单
     */
    public BankQueryResult<BankPayOrderInfo> queryBankPayOrders(BankPayOrderQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        String tenantId = BankQuerySupport.resolveTenant(query.tenantId());
        List<BankPayOrderInfo> rows = BankAppConverter.INSTANCE.toBankPayOrderInfoList(
                bankPayOrderRepository.query(
                        tenantId,
                        BankQuerySupport.blankToNull(query.bankPayOrderNo()),
                        BankQuerySupport.blankToNull(query.accountNo()),
                        BaseEnum.of(PayOrderStatus.class, query.status()),
                        dates.start(),
                        dates.end(),
                        BankDefaults.MAX_QUERY_ROWS));
        return BankQueryResult.ok(rows);
    }

    /**
     * @param query 查询
     * @return 支付明细
     */
    public BankQueryResult<BankPayOrderDetailInfo> queryBankPayOrderDetails(BankPayOrderDetailQuery query) {
        String tenantId = BankQuerySupport.resolveTenant(query.tenantId());
        List<BankPayOrderDetailInfo> rows = BankAppConverter.INSTANCE.toBankPayOrderDetailInfoList(
                bankPayOrderDetailRepository.query(
                        tenantId,
                        BankQuerySupport.blankToNull(query.bankPayOrderNo()),
                        BankQuerySupport.blankToNull(query.bankPayOrderDetailNo()),
                        BankQuerySupport.blankToNull(query.accountNo()),
                        BaseEnum.of(PayOrderStatus.class, query.status())));
        return BankQueryResult.ok(limit(rows));
    }

    /**
     * @param query 查询
     * @return 交易明细
     */
    public BankQueryResult<TradeDetailInfo> queryTradeDetails(TradeDetailQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        String tenantId = BankQuerySupport.resolveTenant(query.tenantId());
        List<TradeDetailInfo> rows = BankAppConverter.INSTANCE.toTradeDetailInfoList(
                tradeDetailRepository.query(
                        tenantId,
                        BankQuerySupport.blankToNull(query.accountNo()),
                        BankQuerySupport.blankToNull(query.tradeDetailNo()),
                        BankQuerySupport.blankToNull(query.receiptNo()),
                        BaseEnum.of(TradeDirection.class, query.direction()),
                        dates.start(),
                        dates.end(),
                        BankDefaults.MAX_QUERY_ROWS));
        return BankQueryResult.ok(rows);
    }

    /**
     * @param query 查询
     * @return 电子回单
     */
    public BankQueryResult<ElectronicReceiptInfo> queryElectronicReceipts(ElectronicReceiptQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        String tenantId = BankQuerySupport.resolveTenant(query.tenantId());
        List<ElectronicReceiptInfo> rows = BankAppConverter.INSTANCE.toElectronicReceiptInfoList(
                electronicReceiptRepository.query(
                        tenantId,
                        BankQuerySupport.blankToNull(query.receiptNo()),
                        BankQuerySupport.blankToNull(query.tradeDetailNo()),
                        BankQuerySupport.blankToNull(query.accountNo()),
                        dates.start(),
                        dates.end()));
        return BankQueryResult.ok(limit(rows));
    }

    /**
     * @param query 查询
     * @return 余额流水
     */
    public BankQueryResult<BalanceFlowInfo> queryBalanceFlows(BalanceFlowQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        String tenantId = BankQuerySupport.resolveTenant(query.tenantId());
        List<BalanceFlowInfo> rows = BankAppConverter.INSTANCE.toBalanceFlowInfoList(
                balanceFlowRepository.query(
                        tenantId,
                        BankQuerySupport.blankToNull(query.accountNo()),
                        dates.start(),
                        dates.end()));
        return BankQueryResult.ok(limit(rows));
    }

    /**
     * @param query 查询
     * @return 电子对账单
     */
    public BankQueryResult<ElectronicStatementInfo> queryElectronicStatements(ElectronicStatementQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.periodStart(), query.periodEnd());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        String tenantId = BankQuerySupport.resolveTenant(query.tenantId());
        List<ElectronicStatementInfo> rows = BankAppConverter.INSTANCE.toElectronicStatementInfoList(
                electronicStatementRepository.query(
                        tenantId,
                        BankQuerySupport.blankToNull(query.statementNo()),
                        BankQuerySupport.blankToNull(query.tradeDetailNo()),
                        BankQuerySupport.blankToNull(query.accountNo()),
                        dates.start(),
                        dates.end()));
        return BankQueryResult.ok(limit(rows));
    }

    private static <T> List<T> limit(List<T> rows) {
        if (rows.size() <= BankDefaults.MAX_QUERY_ROWS) {
            return rows;
        }
        return rows.subList(0, BankDefaults.MAX_QUERY_ROWS);
    }
}
