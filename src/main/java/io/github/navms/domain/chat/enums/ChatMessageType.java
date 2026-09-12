package io.github.navms.domain.chat.enums;

import io.github.navms.domain.chat.exception.BusinessException;
import io.github.navms.domain.chat.exception.ChatErrorCode;
import lombok.Getter;

/**
 * 会话消息类型。
 *
 * @author navms
 */
@Getter
public enum ChatMessageType {

    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool"),
    TOOL_REQUEST("tool-request"),
    TOOL_CONFIRM("tool-confirm");

    private final String code;

    ChatMessageType(String code) {
        this.code = code;
    }

    /**
     * 按类型码解析。
     *
     * @param code 类型码
     * @return 枚举
     */
    public static ChatMessageType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ChatErrorCode.UNSUPPORTED_MESSAGE_TYPE);
        }
        for (ChatMessageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new BusinessException(ChatErrorCode.UNSUPPORTED_MESSAGE_TYPE, "Unsupported message type: " + code);
    }
}
