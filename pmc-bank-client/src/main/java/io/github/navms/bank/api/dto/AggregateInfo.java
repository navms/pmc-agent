package io.github.navms.bank.api.dto;

import java.math.BigDecimal;

/**
 * 汇总一行。
 *
 * @param bucket      分组键
 * @param rowCount    笔数
 * @param totalAmount 金额合计
 * @author navms
 */
public record AggregateInfo(String bucket, long rowCount, BigDecimal totalAmount) {
}
