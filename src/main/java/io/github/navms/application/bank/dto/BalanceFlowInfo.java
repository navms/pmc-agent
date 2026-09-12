package io.github.navms.application.bank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 余额流水信息。
 *
 * @param tenantId      租户 ID
 * @param createdBy     创建人 ID
 * @param id            主键
 * @param flowNo        流水号
 * @param accountNo     账号
 * @param tradeDetailNo 交易明细号
 * @param direction     借贷方向
 * @param changeAmount  变动金额
 * @param balanceBefore 变动前余额
 * @param balanceAfter  变动后余额
 * @param occurTime     发生时间
 * @param bizType       业务类型
 * @param summary       摘要
 * @param createdAt     创建时间
 * @author navms
 */
public record BalanceFlowInfo(
        String tenantId,
        String createdBy,
        Long id,
        String flowNo,
        String accountNo,
        String tradeDetailNo,
        String direction,
        BigDecimal changeAmount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        LocalDateTime occurTime,
        String bizType,
        String summary,
        LocalDateTime createdAt
) {
}
