package io.github.navms.bank.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 交易明细信息。
 *
 * @param tenantId             租户 ID
 * @param createdBy            创建人 ID
 * @param id                   主键
 * @param tradeDetailNo        交易明细号
 * @param receiptNo            回单号
 * @param accountNo            账号
 * @param direction            借贷方向
 * @param amount               金额
 * @param currency             币种
 * @param counterpartAccountNo 对方账号
 * @param counterpartName      对方户名
 * @param counterpartBankName  对方开户行
 * @param summary              摘要
 * @param tradeTime            交易时间
 * @param valueDate            起息日
 * @param balanceAfter         交易后余额
 * @param bankSerialNo         银行流水号
 * @param createdAt            创建时间
 * @author navms
 */
public record TradeDetailInfo(
        String tenantId,
        String createdBy,
        Long id,
        String tradeDetailNo,
        String receiptNo,
        String accountNo,
        String direction,
        BigDecimal amount,
        String currency,
        String counterpartAccountNo,
        String counterpartName,
        String counterpartBankName,
        String summary,
        LocalDateTime tradeTime,
        LocalDate valueDate,
        BigDecimal balanceAfter,
        String bankSerialNo,
        LocalDateTime createdAt
) {
}
