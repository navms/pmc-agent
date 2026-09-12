package io.github.navms.bank.api.dto;

/**
 * 电子对账单查询。
 *
 * @param tenantId      租户 ID
 * @param statementNo   对账单号
 * @param tradeDetailNo 交易明细号
 * @param accountNo     账号
 * @param periodStart   周期起 yyyy-MM-dd
 * @param periodEnd     周期止 yyyy-MM-dd
 * @param maxRows       最大返回行数，空则使用默认查询上限
 * @author navms
 */
public record ElectronicStatementQuery(
        String tenantId,
        String statementNo,
        String tradeDetailNo,
        String accountNo,
        String periodStart,
        String periodEnd,
        Integer maxRows
) {
}
