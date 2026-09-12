package io.github.navms.application.bank.dto;

/**
 * 账户查询。
 *
 * @param tenantId    租户 ID
 * @param accountNo   账号
 * @param accountName 户名
 * @param status      状态码
 * @author navms
 */
public record AccountQuery(String tenantId, String accountNo, String accountName, String status) {
}
