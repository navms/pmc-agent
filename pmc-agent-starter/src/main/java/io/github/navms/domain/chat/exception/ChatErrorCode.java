package io.github.navms.domain.chat.exception;

import lombok.Getter;

/**
 * 会话限界上下文错误码。
 *
 * @author navms
 */
@Getter
public enum ChatErrorCode {

    SESSION_NOT_FOUND("session not found"),
    TITLE_REQUIRED("title cannot be empty"),
    UNSUPPORTED_MESSAGE_TYPE("unsupported message type"),
    MESSAGE_NOT_FOUND("message not found"),
    INVALID_FEEDBACK("invalid feedback"),
    LANGFUSE_UNAVAILABLE("Langfuse is unavailable");

    private final String message;

    ChatErrorCode(String message) {
        this.message = message;
    }

}
