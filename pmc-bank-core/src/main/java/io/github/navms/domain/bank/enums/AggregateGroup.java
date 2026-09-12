package io.github.navms.domain.bank.enums;

import io.github.navms.utils.enums.BaseEnum;
import io.github.navms.utils.enums.StringEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

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
    YEAR("year", "按年"),
    DIRECTION("direction", "按借贷方向"),
    COUNTERPART("counterpart", "按对手方"),
    STATUS("status", "按状态"),
    TYPE("type", "按类型"),
    BANK("bank", "按开户行");

    private final String code;
    private final String name;

    public static AggregateGroup resolveGroup(String groupBy, AggregateGroup fallback) {
        if (!StringUtils.hasText(groupBy)) {
            return fallback;
        }
        AggregateGroup group = BaseEnum.of(AggregateGroup.class, groupBy.trim().toLowerCase());
        return group == null ? fallback : group;
    }

}
