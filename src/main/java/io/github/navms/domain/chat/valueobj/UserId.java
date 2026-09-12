package io.github.navms.domain.chat.valueobj;

/**
 * 用户标识；空值归一为 anonymous。
 *
 * @param value 用户标识
 * @author navms
 */
public record UserId(String value) {

    private static final String ANONYMOUS = "anonymous";

    /**
     * @param value 原始用户标识
     */
    public UserId {
        if (value == null || value.isBlank()) {
            value = ANONYMOUS;
        }
    }

    /**
     * @param raw 原始用户标识
     * @return 值对象
     */
    public static UserId of(String raw) {
        return new UserId(raw);
    }
}
