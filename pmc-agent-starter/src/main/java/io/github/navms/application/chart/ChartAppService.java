package io.github.navms.application.chart;

import io.github.navms.application.chart.dto.ChartCommand;
import io.github.navms.application.chart.dto.ChartInfo;
import io.github.navms.bank.api.BankAggregateApi;
import io.github.navms.bank.api.dto.AggregateInfo;
import io.github.navms.bank.api.dto.BankQueryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于汇总结果组装 ECharts option。
 *
 * @author navms
 */
@Service
@RequiredArgsConstructor
public class ChartAppService {

    private final BankAggregateApi bankAggregateApi;

    /**
     * @param command 图表命令
     * @return 图表
     */
    public ChartInfo createChart(ChartCommand command) {
        String dataset = command.dataset() == null ? "trade" : command.dataset().trim().toLowerCase();
        BankQueryResult<AggregateInfo> result = bankAggregateApi.summarize(dataset, command.query());
        if (!result.success()) {
            return new ChartInfo(false, result.message(), null, Map.of());
        }
        if (result.data().isEmpty()) {
            return new ChartInfo(true, "暂无数据", normalizeChartType(command.chartType()), Map.of());
        }
        String chartType = normalizeChartType(command.chartType());
        String title = StringUtils.hasText(command.title()) ? command.title().trim() : "银企数据图表";
        return new ChartInfo(true, "图表已生成", chartType, toOption(chartType, title, result.data()));
    }

    private static String normalizeChartType(String chartType) {
        if (!StringUtils.hasText(chartType)) {
            return "bar";
        }
        String value = chartType.trim().toLowerCase();
        if ("line".equals(value) || "pie".equals(value) || "bar".equals(value)) {
            return value;
        }
        return "bar";
    }

    private static Map<String, Object> toOption(String chartType, String title, List<AggregateInfo> rows) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("title", Map.of("text", title));
        option.put("tooltip", Map.of("trigger", "pie".equals(chartType) ? "item" : "axis"));
        if ("pie".equals(chartType)) {
            List<Map<String, Object>> data = new ArrayList<>();
            for (AggregateInfo row : rows) {
                data.add(Map.of("name", displayBucket(row.bucket()), "value", amount(row)));
            }
            option.put("legend", Map.of("orient", "vertical", "left", "left"));
            option.put("series", List.of(Map.of(
                    "type", "pie",
                    "radius", "60%",
                    "data", data)));
            return option;
        }
        List<String> xAxis = new ArrayList<>();
        List<BigDecimal> amounts = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (AggregateInfo row : rows) {
            xAxis.add(displayBucket(row.bucket()));
            amounts.add(row.totalAmount() == null ? BigDecimal.ZERO : row.totalAmount());
            counts.add(row.rowCount());
        }
        option.put("legend", Map.of("data", List.of("金额合计", "笔数")));
        option.put("xAxis", Map.of("type", "category", "data", xAxis));
        option.put("yAxis", List.of(Map.of("type", "value", "name", "金额"), Map.of("type", "value", "name", "笔数")));
        option.put("series", List.of(
                Map.of("name", "金额合计", "type", chartType, "data", amounts),
                Map.of("name", "笔数", "type", chartType, "yAxisIndex", 1, "data", counts)));
        return option;
    }

    private static String displayBucket(String bucket) {
        return bucket == null || bucket.isBlank() ? "未知" : bucket;
    }

    private static BigDecimal amount(AggregateInfo row) {
        return row.totalAmount() == null ? BigDecimal.ZERO : row.totalAmount();
    }
}
