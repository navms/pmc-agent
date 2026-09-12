package io.github.navms.domain.bank.enums;

import io.github.navms.utils.enums.StringEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 银行支付单 / 明细状态。
 *
 * @author navms
 */
@Getter
@RequiredArgsConstructor
public enum PayOrderStatus implements StringEnum {

    SUBMITTED("submitted", "已提交"),
    SUCCESS("success", "支付成功"),
    FAILED("failed", "支付失败"),
    RETURNED("returned", "已退回");

    private final String code;

    private final String name;
}
