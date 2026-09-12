package io.github.navms.bank.api.dto;

/**
 * 从银行同步数据。
 *
 * @param tenantId  租户 ID
 * @param accountNo 账号
 * @param startDate 起 yyyy-MM-dd
 * @param endDate   止 yyyy-MM-dd
 * @author navms
 */
public record SyncBankDataCommand(
        String tenantId,
        String accountNo,
        String startDate,
        String endDate
) {
}
