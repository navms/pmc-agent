package io.github.navms.domain.bank.repository;

import io.github.navms.domain.bank.entity.ElectronicReceipt;
import io.github.navms.domain.bank.valueobj.AggregateRow;
import io.github.navms.infrastructure.support.AggregateSql;

import java.time.LocalDate;
import java.util.List;

/**
 * 电子回单查询仓储。
 *
 * @author navms
 */
public interface ElectronicReceiptRepository {

    /**
     * @param tenantId      租户
     * @param receiptNo     回单号，可空
     * @param tradeDetailNo 交易明细号，可空
     * @param accountNo     账号，可空
     * @param startDate     出具日起，可空
     * @param endDate       出具日止，可空
     * @param maxRows       最大行数
     * @return 回单列表
     */
    List<ElectronicReceipt> query(String tenantId, String receiptNo, String tradeDetailNo, String accountNo, LocalDate startDate, LocalDate endDate, int maxRows);

    /**
     * @param tenantId     租户
     * @param accountNo    账号，可空
     * @param startDate    出具日起，可空
     * @param endDate      出具日止，可空
     * @param aggregateSql 聚合 SQL
     * @param maxRows      最大分组数
     * @return 聚合结果
     */
    List<AggregateRow> aggregate(String tenantId, String accountNo, LocalDate startDate, LocalDate endDate, AggregateSql aggregateSql, int maxRows);
}
