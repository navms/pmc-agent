package io.github.navms.domain.chat.enums;

import lombok.Getter;

/**
 * 会话状态。
 *
 * @author navms
 */
@Getter
public enum ChatSessionStatus {

    /**
     * 可继续对话。
     */
    ACTIVE("active"),

    /**
     * 等待人工确认工具调用。
     */
    INTERRUPTED("interrupted");

    private final String code;

    ChatSessionStatus(String code) {
        this.code = code;
    }

    /**
     * 按状态码解析，未知值视为 active。
     *
     * @param code 状态码
     * @return 枚举
     */
    public static ChatSessionStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return ACTIVE;
        }
        for (ChatSessionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return ACTIVE;
    }
}
