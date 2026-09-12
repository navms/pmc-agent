package io.github.navms.domain.bank.enums;

import io.github.navms.utils.enums.StringEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 账户类型。
 *
 * @author navms
 */
@Getter
@RequiredArgsConstructor
public enum AccountType implements StringEnum {

    BASIC("basic", "基本户"),
    SPECIAL("special", "专户"),
    ZERO_BALANCE("zero_balance", "零余额账户");

    private final String code;

    private final String name;
}
