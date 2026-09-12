package io.github.navms.domain.chat.exception;

import lombok.Getter;

/**
 * 会话业务异常。
 *
 * @author navms
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ChatErrorCode code;

    /**
     * @param code 错误码
     */
    public BusinessException(ChatErrorCode code) {
        super(code.getMessage());
        this.code = code;
    }

    /**
     * @param code    错误码
     * @param message 覆盖默认文案
     */
    public BusinessException(ChatErrorCode code, String message) {
        super(message);
        this.code = code;
    }

}
