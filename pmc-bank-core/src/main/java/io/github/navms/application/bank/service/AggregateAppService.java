package io.github.navms.application.bank.service;

import io.github.navms.application.bank.converter.BankAppConverter;
import io.github.navms.bank.api.BankAggregateApi;
import io.github.navms.bank.api.dto.AggregateInfo;
import io.github.navms.bank.api.dto.BankAggregateQuery;
import io.github.navms.bank.api.dto.BankQueryResult;
import io.github.navms.application.bank.support.BankQuerySupport;
import io.github.navms.application.bank.support.BankQuerySupport.DateRange;
import io.github.navms.domain.bank.Defaults;
import io.github.navms.domain.bank.enums.AccountStatus;
import io.github.navms.domain.bank.enums.PayOrderStatus;
import io.github.navms.domain.bank.enums.StatementStatus;
import io.github.navms.domain.bank.enums.TradeDirection;
import io.github.navms.domain.bank.repository.AccountRepository;
import io.github.navms.domain.bank.repository.BankPayOrderDetailRepository;
import io.github.navms.domain.bank.repository.BankPayOrderRepository;
import io.github.navms.domain.bank.repository.ElectronicReceiptRepository;
import io.github.navms.domain.bank.repository.ElectronicStatementRepository;
import io.github.navms.domain.bank.repository.TradeDetailRepository;
import io.github.navms.domain.bank.valueobj.AggregateRow;
import io.github.navms.infrastructure.support.AggregateSql;
import io.github.navms.utils.enums.BaseEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 银企汇总用例，结果来自 SQL 聚合。
 *
 * @author navms
 */
@Service
@RequiredArgsConstructor
public class AggregateAppService implements BankAggregateApi {

    private final AccountRepository accountRepository;

    private final TradeDetailRepository tradeDetailRepository;

    private final BankPayOrderRepository bankPayOrderRepository;

    private final BankPayOrderDetailRepository bankPayOrderDetailRepository;

    private final ElectronicReceiptRepository electronicReceiptRepository;

    private final ElectronicStatementRepository electronicStatementRepository;

    /**
     * @param dataset 数据集
     * @param query   查询
     * @return 汇总
     */
    @Override
    public BankQueryResult<AggregateInfo> summarize(String dataset, BankAggregateQuery query) {
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
    @Override
    public BankQueryResult<AggregateInfo> summarizeTradeDetails(BankAggregateQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        try {
            List<AggregateRow> aggregateRows = tradeDetailRepository.aggregate(
                    BankQuerySupport.resolveTenant(query.tenantId()),
                    BankQuerySupport.blankToNull(query.accountNo()),
                    BaseEnum.of(TradeDirection.class, query.direction()), dates.start(), dates.end(),
                    () -> AggregateSql.tradeDetail(query.groupBy()), Defaults.MAX_AGGREGATE_ROWS);
            return BankQueryResult.ok(BankAppConverter.INSTANCE.toAggregateInfoList(aggregateRows));
        } catch (IllegalArgumentException ex) {
            return BankQueryResult.fail(ex.getMessage());
        }
    }

    /**
     * @param query 查询
     * @return 支付单汇总
     */
    @Override
    public BankQueryResult<AggregateInfo> summarizePayOrders(BankAggregateQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        try {
            List<AggregateRow> aggregateRows = bankPayOrderRepository.aggregate(
                    BankQuerySupport.resolveTenant(query.tenantId()),
                    BankQuerySupport.blankToNull(query.accountNo()),
                    BaseEnum.of(PayOrderStatus.class, query.status()),
                    dates.start(),
                    dates.end(),
                    () -> AggregateSql.payOrder(query.groupBy()), Defaults.MAX_AGGREGATE_ROWS);
            return BankQueryResult.ok(BankAppConverter.INSTANCE.toAggregateInfoList(aggregateRows));
        } catch (IllegalArgumentException ex) {
            return BankQueryResult.fail(ex.getMessage());
        }
    }

    /**
     * @param query 查询
     * @return 支付明细汇总
     */
    @Override
    public BankQueryResult<AggregateInfo> summarizePayOrderDetails(BankAggregateQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        try {
            List<AggregateRow> aggregateRows = bankPayOrderDetailRepository.aggregate(
                    BankQuerySupport.resolveTenant(query.tenantId()),
                    BankQuerySupport.blankToNull(query.accountNo()),
                    BaseEnum.of(PayOrderStatus.class, query.status()),
                    dates.start(),
                    dates.end(),
                    () -> AggregateSql.payOrderDetail(query.groupBy()), Defaults.MAX_AGGREGATE_ROWS);
            return BankQueryResult.ok(BankAppConverter.INSTANCE.toAggregateInfoList(aggregateRows));
        } catch (IllegalArgumentException ex) {
            return BankQueryResult.fail(ex.getMessage());
        }
    }

    /**
     * @param query 查询
     * @return 账户汇总，金额为余额合计
     */
    @Override
    public BankQueryResult<AggregateInfo> summarizeAccounts(BankAggregateQuery query) {
        try {
            List<AggregateRow> aggregateRows = accountRepository.aggregate(
                    BankQuerySupport.resolveTenant(query.tenantId()),
                    BankQuerySupport.blankToNull(query.accountNo()),
                    BaseEnum.of(AccountStatus.class, query.status()),
                    () -> AggregateSql.account(query.groupBy()), Defaults.MAX_AGGREGATE_ROWS);
            return BankQueryResult.ok(BankAppConverter.INSTANCE.toAggregateInfoList(aggregateRows));
        } catch (IllegalArgumentException ex) {
            return BankQueryResult.fail(ex.getMessage());
        }
    }

    /**
     * @param query 查询
     * @return 电子回单汇总
     */
    @Override
    public BankQueryResult<AggregateInfo> summarizeReceipts(BankAggregateQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        try {
            List<AggregateRow> aggregateRows = electronicReceiptRepository.aggregate(
                    BankQuerySupport.resolveTenant(query.tenantId()),
                    BankQuerySupport.blankToNull(query.accountNo()),
                    dates.start(),
                    dates.end(),
                    () -> AggregateSql.receipt(query.groupBy()), Defaults.MAX_AGGREGATE_ROWS);
            return BankQueryResult.ok(BankAppConverter.INSTANCE.toAggregateInfoList(aggregateRows));
        } catch (IllegalArgumentException ex) {
            return BankQueryResult.fail(ex.getMessage());
        }
    }

    /**
     * @param query 查询
     * @return 电子对账单汇总，金额为借贷发生额合计
     */
    @Override
    public BankQueryResult<AggregateInfo> summarizeStatements(BankAggregateQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return BankQueryResult.fail(dates.error());
        }
        try {
            List<AggregateRow> aggregateRows = electronicStatementRepository.aggregate(
                    BankQuerySupport.resolveTenant(query.tenantId()),
                    BankQuerySupport.blankToNull(query.accountNo()),
                    BaseEnum.of(StatementStatus.class, query.status()),
                    dates.start(),
                    dates.end(),
                    () -> AggregateSql.statement(query.groupBy()), Defaults.MAX_AGGREGATE_ROWS);
            return BankQueryResult.ok(BankAppConverter.INSTANCE.toAggregateInfoList(aggregateRows));
        } catch (IllegalArgumentException ex) {
            return BankQueryResult.fail(ex.getMessage());
        }
    }


}
