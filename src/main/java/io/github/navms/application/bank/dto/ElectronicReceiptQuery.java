package io.github.navms.application.bank.dto;

/**
 * 电子回单查询。
 *
 * @param tenantId      租户 ID
 * @param receiptNo     回单号
 * @param tradeDetailNo 交易明细号
 * @param accountNo     账号
 * @param startDate     出具日起 yyyy-MM-dd
 * @param endDate       出具日止 yyyy-MM-dd
 * @author navms
 */
public record ElectronicReceiptQuery(
        String tenantId,
        String receiptNo,
        String tradeDetailNo,
        String accountNo,
        String startDate,
        String endDate
) {
}
