package io.github.navms.bank.api.dto;

/**
 * 银行支付明细查询。
 *
 * @param tenantId             租户 ID
 * @param bankPayOrderNo       支付单号
 * @param bankPayOrderDetailNo 明细号
 * @param accountNo            账号
 * @param status               状态码
 * @param maxRows              最大返回行数，空则使用默认查询上限
 * @author navms
 */
public record BankPayOrderDetailQuery(
        String tenantId,
        String bankPayOrderNo,
        String bankPayOrderDetailNo,
        String accountNo,
        String status,
        Integer maxRows
) {
}
