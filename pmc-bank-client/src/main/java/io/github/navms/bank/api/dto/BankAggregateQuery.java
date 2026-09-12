package io.github.navms.bank.api.dto;

/**
 * 银企汇总查询。
 *
 * @param tenantId  租户 ID
 * @param accountNo 账号
 * @param direction 借贷方向，仅交易明细
 * @param status    支付单状态，仅支付单
 * @param startDate 日起 yyyy-MM-dd
 * @param endDate   日止 yyyy-MM-dd
 * @param groupBy   分组：account / day / month / direction / counterpart / status
 * @author navms
 */
public record BankAggregateQuery(
        String tenantId,
        String accountNo,
        String direction,
        String status,
        String startDate,
        String endDate,
        String groupBy
) {
}
