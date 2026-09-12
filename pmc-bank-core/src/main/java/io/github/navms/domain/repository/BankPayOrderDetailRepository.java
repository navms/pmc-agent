package io.github.navms.domain.bank.repository;

import io.github.navms.domain.bank.entity.BankPayOrderDetail;
import io.github.navms.domain.bank.enums.PayOrderStatus;
import io.github.navms.domain.bank.valueobj.AggregateRow;
import io.github.navms.infrastructure.support.AggregateSql;

import java.time.LocalDate;
import java.util.List;

/**
 * 银行支付明细查询仓储。
 *
 * @author navms
 */
public interface BankPayOrderDetailRepository {

    /**
     * @param tenantId             租户
     * @param bankPayOrderNo       支付单号，可空
     * @param bankPayOrderDetailNo 明细号，可空
     * @param accountNo            账号，可空
     * @param status               状态，可空
     * @param maxRows              最大行数
     * @return 明细列表
     */
    List<BankPayOrderDetail> query(String tenantId, String bankPayOrderNo,
                                   String bankPayOrderDetailNo, String accountNo, PayOrderStatus status, int maxRows);

    /**
     * @param tenantId     租户
     * @param accountNo    账号，可空
     * @param status       状态，可空
     * @param startDate    创建日起，可空
     * @param endDate      创建日止，可空
     * @param aggregateSql 聚合 SQL
     * @param maxRows      最大分组数
     * @return 聚合结果
     */
    List<AggregateRow> aggregate(String tenantId, String accountNo, PayOrderStatus status,
                                 LocalDate startDate, LocalDate endDate, AggregateSql aggregateSql, int maxRows);
}
