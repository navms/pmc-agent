package io.github.navms.domain.chat.valueobj;

/**
 * 会话标题，最长 40 个字符。
 *
 * @param value 标题文本，可为 null
 * @author navms
 */
public record SessionTitle(String value) {

    private static final int MAX_LENGTH = 40;

    /**
     * @param value 原始标题
     */
    public SessionTitle {
        if (value != null) {
            String trimmed = value.strip();
            value = trimmed.length() <= MAX_LENGTH ? trimmed : trimmed.substring(0, MAX_LENGTH);
        }
    }

    /**
     * 用首条用户输入生成标题。
     *
     * @param prompt 用户输入
     * @return 标题
     */
    public static SessionTitle fromPrompt(String prompt) {
        return new SessionTitle(prompt);
    }

    /**
     * @return 是否为空标题
     */
    public boolean isBlank() {
        return value == null || value.isBlank();
    }
}
