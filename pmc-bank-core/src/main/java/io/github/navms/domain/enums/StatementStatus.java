package io.github.navms.domain.bank.enums;

import io.github.navms.utils.enums.StringEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 电子对账单状态。
 *
 * @author navms
 */
@Getter
@RequiredArgsConstructor
public enum StatementStatus implements StringEnum {

    ISSUED("issued", "已出具"),
    CONFIRMED("confirmed", "已确认");

    private final String code;

    private final String name;
}
