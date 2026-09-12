package io.github.navms.application.chart.dto;

import io.github.navms.bank.api.dto.BankAggregateQuery;

/**
 * 图表生成命令。
 *
 * @param dataset   trade / pay_order
 * @param chartType bar / line / pie
 * @param title     图表标题
 * @param query     汇总条件
 * @author navms
 */
public record ChartCommand(String dataset, String chartType, String title, BankAggregateQuery query) {
}
