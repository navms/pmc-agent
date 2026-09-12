package io.github.navms.domain.bank.repository;

import io.github.navms.domain.bank.entity.BankPayOrder;
import io.github.navms.domain.bank.enums.PayOrderStatus;
import io.github.navms.domain.bank.valueobj.AggregateRow;
import io.github.navms.infrastructure.support.AggregateSql;

import java.time.LocalDate;
import java.util.List;

/**
 * 银行支付单查询仓储。
 *
 * @author navms
 */
public interface BankPayOrderRepository {

    /**
     * @param tenantId       租户
     * @param bankPayOrderNo 支付单号，可空
     * @param accountNo      账号，可空
     * @param status         状态，可空
     * @param startDate      申请日起，可空
     * @param endDate        申请日止，可空
     * @param maxRows        最大行数
     * @return 支付单列表
     */
    List<BankPayOrder> query(String tenantId, String bankPayOrderNo, String accountNo, PayOrderStatus status, LocalDate startDate, LocalDate endDate, int maxRows);

    /**
     * @param tenantId     租户
     * @param accountNo    账号，可空
     * @param status       状态，可空
     * @param startDate    申请日起，可空
     * @param endDate      申请日止，可空
     * @param aggregateSql 聚合 SQL
     * @param maxRows      最大分组数
     * @return 聚合结果
     */
    List<AggregateRow> aggregate(String tenantId, String accountNo, PayOrderStatus status, LocalDate startDate, LocalDate endDate, AggregateSql aggregateSql, int maxRows);
}
