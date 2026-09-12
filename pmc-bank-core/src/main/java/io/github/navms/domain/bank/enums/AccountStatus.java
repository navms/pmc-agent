package io.github.navms.domain.bank.enums;

import io.github.navms.utils.enums.StringEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 账户状态。
 *
 * @author navms
 */
@Getter
@RequiredArgsConstructor
public enum AccountStatus implements StringEnum {

    ACTIVE("active", "正常"),
    FROZEN("frozen", "冻结"),
    CLOSED("closed", "销户");

    private final String code;

    private final String name;
}
