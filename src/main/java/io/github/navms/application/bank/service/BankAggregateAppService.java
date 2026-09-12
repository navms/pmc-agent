package io.github.navms.application.bank.service;

import io.github.navms.application.bank.converter.BankAppConverter;
import io.github.navms.application.bank.dto.BankAggregateInfo;
import io.github.navms.application.bank.dto.BankAggregateQuery;
import io.github.navms.application.bank.dto.BankQueryResult;
import io.github.navms.application.bank.support.BankQuerySupport;
import io.github.navms.application.bank.support.BankQuerySupport.DateRange;
import io.github.navms.domain.bank.BankDefaults;
import io.github.navms.domain.bank.enums.AccountStatus;
import io.github.navms.domain.bank.enums.AggregateGroup;
import io.github.navms.domain.bank.enums.PayOrderStatus;
import io.github.navms.domain.bank.enums.StatementStatus;
import io.github.navms.domain.bank.enums.TradeDirection;
import io.github.navms.domain.bank.repository.AccountRepository;
import io.github.navms.domain.bank.repository.BankPayOrderDetailRepository;
import io.github.navms.domain.bank.repository.BankPayOrderRepository;
import io.github.navms.domain.bank.repository.ElectronicReceiptRepository;
import io.github.navms.domain.bank.repository.ElectronicStatementRepository;
import io.github.navms.domain.bank.repository.TradeDetailRepository;
import io.github.navms.utils.enums.BaseEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 银企汇总用例，结果来自 SQL 聚合。
 *
 * @author navms
 */
@Service
@RequiredArgsConstructor
public class BankAggregateAppService {

    private final TradeDetailRepository tradeDetailRepository;

    private final BankPayOrderRepository bankPayOrderRepository;

    private final BankPayOrderDetailRepository bankPayOrderDetailRepository;

    private final AccountRepository accountRepository;

    private final ElectronicReceiptRepository electronicReceiptRepository;

    private final ElectronicStatementRepository electronicStatementRepository;

    /**
     * @param dataset 数据集
     * @param query   查询
     * @return 汇总
     */
    public BankQueryResult<BankAggregateInfo> summarize(String dataset, BankAggregateQuery query) {
        String value = dataset == null ? "trade" : dataset.trim().toLowerCase();
        return switch (value) {
            case "pay_order", "payorder" -> summarizePayOrders(query);
            case "pay_order_detail", "payorderdetail" -> summarizePayOrderDetails(query);
            case "account" -> summarizeAccounts(query);
            case "receipt" -> summarizeReceipts(query);
            case "statement" -> summarizeStatements(query);
            default -> summarizeTradeDetails(query);
        };
    }

    /**
     * @param query 查询
     * @return 交易明细汇总
     */
    public BankQueryResult<BankAggregateInfo> summarizeTradeDetails(BankAggregateQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        AggregateGroup group = resolveGroup(query.groupBy(), AggregateGroup.ACCOUNT);
        try {
            List<BankAggregateInfo> rows = BankAppConverter.INSTANCE.toBankAggregateInfoList(
                    tradeDetailRepository.aggregate(
                            BankQuerySupport.resolveTenant(query.tenantId()),
                            BankQuerySupport.blankToNull(query.accountNo()),
                            BaseEnum.of(TradeDirection.class, query.direction()),
                            dates.start(),
                            dates.end(),
                            group,
                            BankDefaults.MAX_AGGREGATE_ROWS));
            return BankQueryResult.ok(rows);
        } catch (IllegalArgumentException ex) {
            return BankQueryResult.fail(ex.getMessage());
        }
    }

    /**
     * @param query 查询
     * @return 支付单汇总
     */
    public BankQueryResult<BankAggregateInfo> summarizePayOrders(BankAggregateQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        AggregateGroup group = resolveGroup(query.groupBy(), AggregateGroup.STATUS);
        try {
            List<BankAggregateInfo> rows = BankAppConverter.INSTANCE.toBankAggregateInfoList(
                    bankPayOrderRepository.aggregate(
                            BankQuerySupport.resolveTenant(query.tenantId()),
                            BankQuerySupport.blankToNull(query.accountNo()),
                            BaseEnum.of(PayOrderStatus.class, query.status()),
                            dates.start(),
                            dates.end(),
                            group,
                            BankDefaults.MAX_AGGREGATE_ROWS));
            return BankQueryResult.ok(rows);
        } catch (IllegalArgumentException ex) {
            return BankQueryResult.fail(ex.getMessage());
        }
    }

    /**
     * @param query 查询
     * @return 支付明细汇总
     */
    public BankQueryResult<BankAggregateInfo> summarizePayOrderDetails(BankAggregateQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        AggregateGroup group = resolveGroup(query.groupBy(), AggregateGroup.STATUS);
        try {
            List<BankAggregateInfo> rows = BankAppConverter.INSTANCE.toBankAggregateInfoList(
                    bankPayOrderDetailRepository.aggregate(
                            BankQuerySupport.resolveTenant(query.tenantId()),
                            BankQuerySupport.blankToNull(query.accountNo()),
                            BaseEnum.of(PayOrderStatus.class, query.status()),
                            dates.start(),
                            dates.end(),
                            group,
                            BankDefaults.MAX_AGGREGATE_ROWS));
            return BankQueryResult.ok(rows);
        } catch (IllegalArgumentException ex) {
            return BankQueryResult.fail(ex.getMessage());
        }
    }

    /**
     * @param query 查询
     * @return 账户汇总，金额为余额合计
     */
    public BankQueryResult<BankAggregateInfo> summarizeAccounts(BankAggregateQuery query) {
        AggregateGroup group = resolveGroup(query.groupBy(), AggregateGroup.STATUS);
        try {
            List<BankAggregateInfo> rows = BankAppConverter.INSTANCE.toBankAggregateInfoList(
                    accountRepository.aggregate(
                            BankQuerySupport.resolveTenant(query.tenantId()),
                            BankQuerySupport.blankToNull(query.accountNo()),
                            BaseEnum.of(AccountStatus.class, query.status()),
                            group,
                            BankDefaults.MAX_AGGREGATE_ROWS));
            return BankQueryResult.ok(rows);
        } catch (IllegalArgumentException ex) {
            return BankQueryResult.fail(ex.getMessage());
        }
    }

    /**
     * @param query 查询
     * @return 电子回单汇总
     */
    public BankQueryResult<BankAggregateInfo> summarizeReceipts(BankAggregateQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        AggregateGroup group = resolveGroup(query.groupBy(), AggregateGroup.ACCOUNT);
        try {
            List<BankAggregateInfo> rows = BankAppConverter.INSTANCE.toBankAggregateInfoList(
                    electronicReceiptRepository.aggregate(
                            BankQuerySupport.resolveTenant(query.tenantId()),
                            BankQuerySupport.blankToNull(query.accountNo()),
                            dates.start(),
                            dates.end(),
                            group,
                            BankDefaults.MAX_AGGREGATE_ROWS));
            return BankQueryResult.ok(rows);
        } catch (IllegalArgumentException ex) {
            return BankQueryResult.fail(ex.getMessage());
        }
    }

    /**
     * @param query 查询
     * @return 电子对账单汇总，金额为借贷发生额合计
     */
    public BankQueryResult<BankAggregateInfo> summarizeStatements(BankAggregateQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        AggregateGroup group = resolveGroup(query.groupBy(), AggregateGroup.ACCOUNT);
        try {
            List<BankAggregateInfo> rows = BankAppConverter.INSTANCE.toBankAggregateInfoList(
                    electronicStatementRepository.aggregate(
                            BankQuerySupport.resolveTenant(query.tenantId()),
                            BankQuerySupport.blankToNull(query.accountNo()),
                            BaseEnum.of(StatementStatus.class, query.status()),
                            dates.start(),
                            dates.end(),
                            group,
                            BankDefaults.MAX_AGGREGATE_ROWS));
            return BankQueryResult.ok(rows);
        } catch (IllegalArgumentException ex) {
            return BankQueryResult.fail(ex.getMessage());
        }
    }

    private static AggregateGroup resolveGroup(String groupBy, AggregateGroup fallback) {
        if (!StringUtils.hasText(groupBy)) {
            return fallback;
        }
        AggregateGroup group = BaseEnum.of(AggregateGroup.class, groupBy.trim().toLowerCase());
        return group == null ? fallback : group;
    }
}
