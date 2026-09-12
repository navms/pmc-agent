package io.github.navms.bank.api.dto;

/**
 * 账户查询。
 *
 * @param tenantId    租户 ID
 * @param accountNo   账号
 * @param accountName 户名
 * @param status      状态码
 * @param maxRows     最大返回行数，空则使用默认查询上限
 * @author navms
 */
public record AccountQuery(
        String tenantId,
        String accountNo,
        String accountName,
        String status,
        Integer maxRows
) {
}
