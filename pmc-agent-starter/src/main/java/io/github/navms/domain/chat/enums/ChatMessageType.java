package io.github.navms.domain.chat.enums;

import io.github.navms.domain.chat.exception.BusinessException;
import io.github.navms.domain.chat.exception.ChatErrorCode;
import lombok.Getter;

/**
 * AG-UI Message.role。
 *
 * @author navms
 */
@Getter
public enum ChatMessageType {

    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool"),
    REASONING("reasoning"),
    ACTIVITY("activity");

    private final String code;

    ChatMessageType(String code) {
        this.code = code;
    }

    /**
     * @param code role
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
