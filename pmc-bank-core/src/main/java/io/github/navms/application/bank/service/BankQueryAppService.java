package io.github.navms.application.bank.service;

import io.github.navms.application.bank.converter.BankAppConverter;
import io.github.navms.application.bank.support.BankQuerySupport;
import io.github.navms.application.bank.support.BankQuerySupport.DateRange;
import io.github.navms.bank.api.BankQueryApi;
import io.github.navms.bank.api.dto.AccountInfo;
import io.github.navms.bank.api.dto.AccountQuery;
import io.github.navms.bank.api.dto.BalanceFlowInfo;
import io.github.navms.bank.api.dto.BalanceFlowQuery;
import io.github.navms.bank.api.dto.BankPayOrderDetailInfo;
import io.github.navms.bank.api.dto.BankPayOrderDetailQuery;
import io.github.navms.bank.api.dto.BankPayOrderInfo;
import io.github.navms.bank.api.dto.BankPayOrderQuery;
import io.github.navms.bank.api.dto.BankQueryResult;
import io.github.navms.bank.api.dto.ElectronicReceiptInfo;
import io.github.navms.bank.api.dto.ElectronicReceiptQuery;
import io.github.navms.bank.api.dto.ElectronicStatementInfo;
import io.github.navms.bank.api.dto.ElectronicStatementQuery;
import io.github.navms.bank.api.dto.TradeDetailInfo;
import io.github.navms.bank.api.dto.TradeDetailQuery;
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
public class BankQueryAppService implements BankQueryApi {

    private final AccountRepository accountRepository;

    private final BankPayOrderRepository bankPayOrderRepository;

    private final BankPayOrderDetailRepository bankPayOrderDetailRepository;

    private final TradeDetailRepository tradeDetailRepository;

    private final ElectronicReceiptRepository electronicReceiptRepository;

    private final BalanceFlowRepository balanceFlowRepository;

    private final ElectronicStatementRepository electronicStatementRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public BankQueryResult<AccountInfo> queryAccounts(AccountQuery query) {
        List<AccountInfo> rows = BankAppConverter.INSTANCE.toAccountInfoList(
                accountRepository.query(
                        BankQuerySupport.resolveTenant(query.tenantId()),
                        BankQuerySupport.blankToNull(query.accountNo()),
                        BankQuerySupport.blankToNull(query.accountName()),
                        BaseEnum.of(AccountStatus.class, query.status()),
                        BankQuerySupport.resolveMaxRows(query.maxRows())));
        return BankQueryResult.ok(rows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BankQueryResult<BankPayOrderInfo> queryBankPayOrders(BankPayOrderQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        List<BankPayOrderInfo> rows = BankAppConverter.INSTANCE.toBankPayOrderInfoList(
                bankPayOrderRepository.query(
                        BankQuerySupport.resolveTenant(query.tenantId()),
                        BankQuerySupport.blankToNull(query.bankPayOrderNo()),
                        BankQuerySupport.blankToNull(query.accountNo()),
                        BaseEnum.of(PayOrderStatus.class, query.status()),
                        dates.start(),
                        dates.end(),
                        BankQuerySupport.resolveMaxRows(query.maxRows())));
        return BankQueryResult.ok(rows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BankQueryResult<BankPayOrderDetailInfo> queryBankPayOrderDetails(BankPayOrderDetailQuery query) {
        List<BankPayOrderDetailInfo> rows = BankAppConverter.INSTANCE.toBankPayOrderDetailInfoList(
                bankPayOrderDetailRepository.query(
                        BankQuerySupport.resolveTenant(query.tenantId()),
                        BankQuerySupport.blankToNull(query.bankPayOrderNo()),
                        BankQuerySupport.blankToNull(query.bankPayOrderDetailNo()),
                        BankQuerySupport.blankToNull(query.accountNo()),
                        BaseEnum.of(PayOrderStatus.class, query.status()),
                        BankQuerySupport.resolveMaxRows(query.maxRows())));
        return BankQueryResult.ok(rows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BankQueryResult<TradeDetailInfo> queryTradeDetails(TradeDetailQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        List<TradeDetailInfo> rows = BankAppConverter.INSTANCE.toTradeDetailInfoList(
                tradeDetailRepository.query(
                        BankQuerySupport.resolveTenant(query.tenantId()),
                        BankQuerySupport.blankToNull(query.accountNo()),
                        BankQuerySupport.blankToNull(query.tradeDetailNo()),
                        BankQuerySupport.blankToNull(query.receiptNo()),
                        BaseEnum.of(TradeDirection.class, query.direction()),
                        dates.start(),
                        dates.end(),
                        BankQuerySupport.resolveMaxRows(query.maxRows())));
        return BankQueryResult.ok(rows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BankQueryResult<ElectronicReceiptInfo> queryElectronicReceipts(ElectronicReceiptQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        List<ElectronicReceiptInfo> rows = BankAppConverter.INSTANCE.toElectronicReceiptInfoList(
                electronicReceiptRepository.query(
                        BankQuerySupport.resolveTenant(query.tenantId()),
                        BankQuerySupport.blankToNull(query.receiptNo()),
                        BankQuerySupport.blankToNull(query.tradeDetailNo()),
                        BankQuerySupport.blankToNull(query.accountNo()),
                        dates.start(),
                        dates.end(),
                        BankQuerySupport.resolveMaxRows(query.maxRows())));
        return BankQueryResult.ok(rows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BankQueryResult<BalanceFlowInfo> queryBalanceFlows(BalanceFlowQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        List<BalanceFlowInfo> rows = BankAppConverter.INSTANCE.toBalanceFlowInfoList(
                balanceFlowRepository.query(
                        BankQuerySupport.resolveTenant(query.tenantId()),
                        BankQuerySupport.blankToNull(query.accountNo()),
                        dates.start(),
                        dates.end(),
                        BankQuerySupport.resolveMaxRows(query.maxRows())));
        return BankQueryResult.ok(rows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BankQueryResult<ElectronicStatementInfo> queryElectronicStatements(ElectronicStatementQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.periodStart(), query.periodEnd());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        List<ElectronicStatementInfo> rows = BankAppConverter.INSTANCE.toElectronicStatementInfoList(
                electronicStatementRepository.query(
                        BankQuerySupport.resolveTenant(query.tenantId()),
                        BankQuerySupport.blankToNull(query.statementNo()),
                        BankQuerySupport.blankToNull(query.tradeDetailNo()),
                        BankQuerySupport.blankToNull(query.accountNo()),
                        dates.start(),
                        dates.end(),
                        BankQuerySupport.resolveMaxRows(query.maxRows())));
        return BankQueryResult.ok(rows);
    }

}
