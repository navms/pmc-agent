package io.github.navms.domain.bank.repository;

import io.github.navms.domain.bank.entity.TradeDetail;
import io.github.navms.domain.bank.enums.AggregateGroup;
import io.github.navms.domain.bank.enums.TradeDirection;
import io.github.navms.domain.bank.valueobj.BankAggregateRow;

import java.time.LocalDate;
import java.util.List;

/**
 * 交易明细查询仓储。
 *
 * @author navms
 */
public interface TradeDetailRepository {

    /**
     * @param tenantId      租户
     * @param accountNo     账号，可空
     * @param tradeDetailNo 交易明细号，可空
     * @param receiptNo     回单号，可空
     * @param direction     借贷方向，可空
     * @param startDate     交易日起，可空
     * @param endDate       交易日止，可空
     * @param maxRows       最大行数
     * @return 交易明细列表
     */
    List<TradeDetail> query(
            String tenantId,
            String accountNo,
            String tradeDetailNo,
            String receiptNo,
            TradeDirection direction,
            LocalDate startDate,
            LocalDate endDate,
            int maxRows);

    /**
     * @param tenantId  租户
     * @param accountNo 账号，可空
     * @param direction 借贷方向，可空
     * @param startDate 交易日起，可空
     * @param endDate   交易日止，可空
     * @param group     分组
     * @param maxRows   最大分组数
     * @return 聚合结果
     */
    List<BankAggregateRow> aggregate(
            String tenantId,
            String accountNo,
            TradeDirection direction,
            LocalDate startDate,
            LocalDate endDate,
            AggregateGroup group,
            int maxRows);
}
