package io.github.navms.bank.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 电子对账单信息。
 *
 * @param tenantId       租户 ID
 * @param createdBy      创建人 ID
 * @param id             主键
 * @param statementNo    对账单号
 * @param tradeDetailNo  交易明细号
 * @param accountNo      账号
 * @param periodStart    周期起
 * @param periodEnd      周期止
 * @param openingBalance 期初余额
 * @param closingBalance 期末余额
 * @param debitTotal     借方合计
 * @param creditTotal    贷方合计
 * @param status         状态
 * @param issueTime      出具时间
 * @param createdAt      创建时间
 * @author navms
 */
public record ElectronicStatementInfo(
        String tenantId,
        String createdBy,
        Long id,
        String statementNo,
        String tradeDetailNo,
        String accountNo,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        BigDecimal debitTotal,
        BigDecimal creditTotal,
        String status,
        LocalDateTime issueTime,
        LocalDateTime createdAt
) {
}
