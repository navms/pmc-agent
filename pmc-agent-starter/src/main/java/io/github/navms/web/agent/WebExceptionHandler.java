package io.github.navms.web.agent;

import io.github.navms.domain.chat.exception.BusinessException;
import io.github.navms.domain.chat.exception.ChatErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 将会话业务异常映射为既有 HTTP 语义。
 *
 * @author navms
 */
@RestControllerAdvice
public class WebExceptionHandler {

    /**
     * @param exception 业务异常
     * @return 对应 HTTP 状态
     */
    public static ResponseStatusException toResponseStatus(BusinessException exception) {
        return new ResponseStatusException(statusOf(exception.getCode()), exception.getMessage(), exception);
    }

    /**
     * @param code 错误码
     * @return HTTP 状态
     */
    public static HttpStatus statusOf(ChatErrorCode code) {
        if (code == ChatErrorCode.SESSION_NOT_FOUND || code == ChatErrorCode.MESSAGE_NOT_FOUND) {
            return HttpStatus.NOT_FOUND;
        }
        if (code == ChatErrorCode.LANGFUSE_UNAVAILABLE) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.BAD_REQUEST;
    }

    /**
     * @param exception 业务异常
     * @return HTTP 响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(statusOf(exception.getCode())).body(exception.getMessage());
    }
}
