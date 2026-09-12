package io.github.navms.bank.api.dto;

/**
 * 银行同步结果。
 *
 * @param mocked      是否 mock
 * @param tenantId    租户 ID
 * @param accountNo   账号
 * @param dataType    数据类型
 * @param startDate   起 yyyy-MM-dd
 * @param endDate     止 yyyy-MM-dd
 * @param syncedCount 同步笔数
 * @author navms
 */
public record BankSyncInfo(
        boolean mocked,
        String tenantId,
        String accountNo,
        String dataType,
        String startDate,
        String endDate,
        int syncedCount
) {
}
