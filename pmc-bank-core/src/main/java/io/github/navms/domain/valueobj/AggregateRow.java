package io.github.navms.domain.bank.valueobj;

import java.math.BigDecimal;

/**
 * SQL 聚合一行。
 *
 * @param bucket      分组键
 * @param rowCount    笔数
 * @param totalAmount 金额合计
 * @author navms
 */
public record AggregateRow(String bucket, long rowCount, BigDecimal totalAmount) {
}
