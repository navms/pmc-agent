package io.github.navms.application.bank.dto;

import java.math.BigDecimal;

/**
 * 汇总一行。
 *
 * @param bucket      分组键
 * @param rowCount    笔数
 * @param totalAmount 金额合计
 * @author navms
 */
public record BankAggregateInfo(String bucket, long rowCount, BigDecimal totalAmount) {
}
