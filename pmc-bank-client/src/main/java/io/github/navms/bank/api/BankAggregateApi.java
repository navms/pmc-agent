package io.github.navms.bank.api;

import io.github.navms.bank.api.dto.AggregateInfo;
import io.github.navms.bank.api.dto.BankAggregateQuery;
import io.github.navms.bank.api.dto.BankQueryResult;

/**
 * 银企直连汇总 API。
 *
 * @author navms
 */
public interface BankAggregateApi {

    /**
     * @param dataset 数据集 trade / pay_order / pay_order_detail / account / receipt / statement
     * @param query   查询
     * @return 汇总
     */
    BankQueryResult<AggregateInfo> summarize(String dataset, BankAggregateQuery query);

    /**
     * @param query 查询
     * @return 交易明细汇总
     */
    BankQueryResult<AggregateInfo> summarizeTradeDetails(BankAggregateQuery query);

    /**
     * @param query 查询
     * @return 支付单汇总
     */
    BankQueryResult<AggregateInfo> summarizePayOrders(BankAggregateQuery query);

    /**
     * @param query 查询
     * @return 支付明细汇总
     */
    BankQueryResult<AggregateInfo> summarizePayOrderDetails(BankAggregateQuery query);

    /**
     * @param query 查询
     * @return 账户汇总
     */
    BankQueryResult<AggregateInfo> summarizeAccounts(BankAggregateQuery query);

    /**
     * @param query 查询
     * @return 电子回单汇总
     */
    BankQueryResult<AggregateInfo> summarizeReceipts(BankAggregateQuery query);

    /**
     * @param query 查询
     * @return 电子对账单汇总
     */
    BankQueryResult<AggregateInfo> summarizeStatements(BankAggregateQuery query);
}
