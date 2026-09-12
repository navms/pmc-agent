package io.github.navms.application.bank.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账户信息。
 *
 * @param tenantId          租户 ID
 * @param createdBy         创建人 ID
 * @param accountNo         账号
 * @param accountName       户名
 * @param bankCode          银行代码
 * @param bankName          开户行
 * @param branchName        网点
 * @param currency          币种
 * @param accountType       账户类型
 * @param status            状态
 * @param balance           当前余额
 * @param availableBalance  可用余额
 * @param frozenAmount      冻结金额
 * @param openDate          开户日期
 * @param remark            备注
 * @param createdAt         创建时间
 * @author navms
 */
public record AccountInfo(
        String tenantId,
        String createdBy,
        String accountNo,
        String accountName,
        String bankCode,
        String bankName,
        String branchName,
        String currency,
        String accountType,
        String status,
        BigDecimal balance,
        BigDecimal availableBalance,
        BigDecimal frozenAmount,
        LocalDate openDate,
        String remark,
        LocalDateTime createdAt
) {
}
