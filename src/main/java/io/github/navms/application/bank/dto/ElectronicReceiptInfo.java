package io.github.navms.application.bank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 电子回单信息。
 *
 * @param tenantId      租户 ID
 * @param createdBy     创建人 ID
 * @param id            主键
 * @param receiptNo     回单号
 * @param tradeDetailNo 交易明细号
 * @param accountNo     账号
 * @param amount        金额
 * @param currency      币种
 * @param payerName     付款方
 * @param payeeName     收款方
 * @param issueTime     出具时间
 * @param bankName      银行
 * @param digest        摘要
 * @param createdAt     创建时间
 * @author navms
 */
public record ElectronicReceiptInfo(
        String tenantId,
        String createdBy,
        Long id,
        String receiptNo,
        String tradeDetailNo,
        String accountNo,
        BigDecimal amount,
        String currency,
        String payerName,
        String payeeName,
        LocalDateTime issueTime,
        String bankName,
        String digest,
        LocalDateTime createdAt
) {
}
