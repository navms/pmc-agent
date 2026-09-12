package io.github.navms.application.bank.support;

import io.github.navms.domain.bank.Defaults;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 银企查询公共解析。
 *
 * @author navms
 */
public final class BankQuerySupport {

    /**
     * @param tenantId 入参租户
     * @return 有效租户
     */
    public static String resolveTenant(String tenantId) {
        return StringUtils.hasText(tenantId) ? tenantId.trim() : Defaults.DEFAULT_TENANT_ID;
    }

    /**
     * @param maxRows 调用方指定上限
     * @return 有效行数，未指定则默认查询上限，且不超过导出上限
     */
    public static int resolveMaxRows(Integer maxRows) {
        if (maxRows == null || maxRows <= 0) {
            return Defaults.MAX_QUERY_ROWS;
        }
        return Math.min(maxRows, Defaults.MAX_EXPORT_ROWS + 1);
    }

    /**
     * @param value 原始字符串
     * @return 空白则为 null
     */
    public static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * @param startDate 起
     * @param endDate   止
     * @return 日期区间
     */
    public static DateRange parseRange(String startDate, String endDate) {
        try {
            return new DateRange(parseDate(startDate), parseDate(endDate), null);
        } catch (DateTimeParseException ex) {
            return new DateRange(null, null, "日期格式须为 yyyy-MM-dd，请补充正确的日期后再查询");
        }
    }

    private static LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    /**
     * @param start 起
     * @param end   止
     * @param error 错误说明
     * @author navms
     */
    public record DateRange(LocalDate start, LocalDate end, String error) {
    }
}
