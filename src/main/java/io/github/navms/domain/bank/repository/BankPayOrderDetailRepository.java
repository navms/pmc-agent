package io.github.navms.domain.bank.repository;

import io.github.navms.domain.bank.entity.BankPayOrderDetail;
import io.github.navms.domain.bank.enums.AggregateGroup;
import io.github.navms.domain.bank.enums.PayOrderStatus;
import io.github.navms.domain.bank.valueobj.BankAggregateRow;

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
     * @return 明细列表
     */
    List<BankPayOrderDetail> query(
            String tenantId,
            String bankPayOrderNo,
            String bankPayOrderDetailNo,
            String accountNo,
            PayOrderStatus status);

    /**
     * @param tenantId  租户
     * @param accountNo 账号，可空
     * @param status    状态，可空
     * @param startDate 创建日起，可空
     * @param endDate   创建日止，可空
     * @param group     分组
     * @param maxRows   最大分组数
     * @return 聚合结果
     */
    List<BankAggregateRow> aggregate(
            String tenantId,
            String accountNo,
            PayOrderStatus status,
            LocalDate startDate,
            LocalDate endDate,
            AggregateGroup group,
            int maxRows);
}
