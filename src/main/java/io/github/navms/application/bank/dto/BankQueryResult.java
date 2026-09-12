package io.github.navms.application.bank.dto;

import java.util.List;

/**
 * 查询结果，供 Tool 序列化为 JSON。
 *
 * @param success 是否成功
 * @param message 说明
 * @param data    数据
 * @param <T>     行类型
 * @author navms
 */
public record BankQueryResult<T>(boolean success, String message, List<T> data) {

    /**
     * @param data 数据
     * @param <T>  行类型
     * @return 成功结果
     */
    public static <T> BankQueryResult<T> ok(List<T> data) {
        if (data == null || data.isEmpty()) {
            return new BankQueryResult<>(true, "暂无数据", List.of());
        }
        return new BankQueryResult<>(true, "查询成功", data);
    }

    /**
     * @param message 提示
     * @param <T>     行类型
     * @return 成功但需补充条件
     */
    public static <T> BankQueryResult<T> hint(String message) {
        return new BankQueryResult<>(true, message, List.of());
    }

    /**
     * @param message 失败说明
     * @param <T>     行类型
     * @return 失败结果
     */
    public static <T> BankQueryResult<T> fail(String message) {
        return new BankQueryResult<>(false, message, List.of());
    }
}
