package io.github.navms.domain.bank.enums;

import io.github.navms.utils.enums.StringEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 交易借贷方向。
 *
 * @author navms
 */
@Getter
@RequiredArgsConstructor
public enum TradeDirection implements StringEnum {

    DEBIT("debit", "借/支出"),
    CREDIT("credit", "贷/收入");

    private final String code;

    private final String name;
}
