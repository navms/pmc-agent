package io.github.navms.bank.api.dto;

/**
 * 写操作结果，供 Tool 序列化为 JSON。
 *
 * @param success 是否成功
 * @param message 说明
 * @param data    数据
 * @param <T>     载荷类型
 * @author navms
 */
public record BankWriteResult<T>(boolean success, String message, T data) {

    /**
     * @param data    数据
     * @param message 说明
     * @param <T>     载荷类型
     * @return 成功
     */
    public static <T> BankWriteResult<T> ok(T data, String message) {
        return new BankWriteResult<>(true, message, data);
    }

    /**
     * @param message 失败说明
     * @param <T>     载荷类型
     * @return 失败
     */
    public static <T> BankWriteResult<T> fail(String message) {
        return new BankWriteResult<>(false, message, null);
    }
}
