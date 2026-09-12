package io.github.navms.domain.bank.enums;

import io.github.navms.utils.enums.StringEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 银企汇总分组维度。
 *
 * @author navms
 */
@Getter
@RequiredArgsConstructor
public enum AggregateGroup implements StringEnum {

    ACCOUNT("account", "按账号"),
    DAY("day", "按日"),
    MONTH("month", "按月"),
    DIRECTION("direction", "按借贷方向"),
    COUNTERPART("counterpart", "按对手方"),
    STATUS("status", "按状态"),
    TYPE("type", "按类型"),
    BANK("bank", "按开户行");

    private final String code;

    private final String name;
}
