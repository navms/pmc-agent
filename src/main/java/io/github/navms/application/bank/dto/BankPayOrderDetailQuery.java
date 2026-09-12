package io.github.navms.application.bank.dto;

/**
 * 银行支付明细查询。
 *
 * @param tenantId             租户 ID
 * @param bankPayOrderNo       支付单号
 * @param bankPayOrderDetailNo 明细号
 * @param accountNo            账号
 * @param status               状态码
 * @author navms
 */
public record BankPayOrderDetailQuery(
        String tenantId,
        String bankPayOrderNo,
        String bankPayOrderDetailNo,
        String accountNo,
        String status
) {
}
