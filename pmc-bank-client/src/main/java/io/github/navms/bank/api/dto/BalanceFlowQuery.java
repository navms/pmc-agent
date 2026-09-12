package io.github.navms.bank.api.dto;

/**
 * 余额流水查询。
 *
 * @param tenantId  租户 ID
 * @param accountNo 账号
 * @param startDate 发生日起 yyyy-MM-dd
 * @param endDate   发生日止 yyyy-MM-dd
 * @param maxRows   最大返回行数，空则使用默认查询上限
 * @author navms
 */
public record BalanceFlowQuery(
        String tenantId,
        String accountNo,
        String startDate,
        String endDate,
        Integer maxRows
) {
}
