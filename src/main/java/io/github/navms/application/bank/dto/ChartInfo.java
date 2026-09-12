package io.github.navms.application.bank.dto;

import java.util.Map;

/**
 * 图表结果，option 为 ECharts 配置。
 *
 * @param success   是否成功
 * @param message   说明
 * @param chartType 图表类型
 * @param option    ECharts option
 * @author navms
 */
public record ChartInfo(boolean success, String message, String chartType, Map<String, Object> option) {
}
