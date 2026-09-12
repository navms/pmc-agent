package io.github.navms.application.bank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 银行支付明细信息。
 *
 * @param tenantId             租户 ID
 * @param createdBy            创建人 ID
 * @param id                   主键
 * @param bankPayOrderId       支付单主键
 * @param bankPayOrderNo       支付单号
 * @param bankPayOrderDetailNo 明细号
 * @param accountNo            付款账号
 * @param payeeAccountNo       收款账号
 * @param payeeName            收款户名
 * @param payeeBankName        收款开户行
 * @param amount               金额
 * @param currency             币种
 * @param status               状态
 * @param usage                用途
 * @param seqNo                序号
 * @param failReason           失败原因
 * @param createdAt            创建时间
 * @author navms
 */
public record BankPayOrderDetailInfo(
        String tenantId,
        String createdBy,
        Long id,
        Long bankPayOrderId,
        String bankPayOrderNo,
        String bankPayOrderDetailNo,
        String accountNo,
        String payeeAccountNo,
        String payeeName,
        String payeeBankName,
        BigDecimal amount,
        String currency,
        String status,
        String usage,
        Integer seqNo,
        String failReason,
        LocalDateTime createdAt
) {
}
