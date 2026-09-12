package io.github.navms.application.bank.dto;

/**
 * 银行支付单查询。
 *
 * @param tenantId       租户 ID
 * @param bankPayOrderNo 支付单号
 * @param accountNo      账号
 * @param status         状态码
 * @param startDate      申请日起 yyyy-MM-dd
 * @param endDate        申请日止 yyyy-MM-dd
 * @author navms
 */
public record BankPayOrderQuery(
        String tenantId,
        String bankPayOrderNo,
        String accountNo,
        String status,
        String startDate,
        String endDate
) {
}
