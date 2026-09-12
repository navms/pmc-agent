package io.github.navms.domain.bank.exception;

import lombok.Getter;

/**
 * 银企直连限界上下文错误码。
 *
 * @author navms
 */
@Getter
public enum BankErrorCode {

    TENANT_REQUIRED("tenantId cannot be empty");

    private final String message;

    BankErrorCode(String message) {
        this.message = message;
    }
}
