package io.github.navms.domain.bank.repository;

import io.github.navms.domain.bank.entity.ElectronicStatement;
import io.github.navms.domain.bank.enums.StatementStatus;
import io.github.navms.domain.bank.valueobj.AggregateRow;
import io.github.navms.infrastructure.support.AggregateSql;

import java.time.LocalDate;
import java.util.List;

/**
 * 电子对账单查询仓储。
 *
 * @author navms
 */
public interface ElectronicStatementRepository {

    /**
     * @param tenantId      租户
     * @param statementNo   对账单号，可空
     * @param tradeDetailNo 交易明细号，可空
     * @param accountNo     账号，可空
     * @param periodStart   周期起，可空
     * @param periodEnd     周期止，可空
     * @param maxRows       最大行数
     * @return 对账单列表
     */
    List<ElectronicStatement> query(String tenantId, String statementNo, String tradeDetailNo,
                                    String accountNo, LocalDate periodStart, LocalDate periodEnd, int maxRows);

    /**
     * @param tenantId     租户
     * @param accountNo    账号，可空
     * @param status       状态，可空
     * @param startDate    出具日起，可空
     * @param endDate      出具日止，可空
     * @param aggregateSql 聚合 SQL
     * @param maxRows      最大分组数
     * @return 聚合结果，金额为借贷发生额合计
     */
    List<AggregateRow> aggregate(String tenantId, String accountNo, StatementStatus status,
                                 LocalDate startDate, LocalDate endDate, AggregateSql aggregateSql, int maxRows);
}
