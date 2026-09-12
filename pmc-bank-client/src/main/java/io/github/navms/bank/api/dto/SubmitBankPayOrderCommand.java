package io.github.navms.bank.api.dto;

import java.math.BigDecimal;

/**
 * 提交银行支付单。
 *
 * @param tenantId       租户 ID
 * @param bankPayOrderNo 支付单号，可空则由系统生成
 * @param accountNo      付款账号
 * @param totalAmount    合计金额
 * @param totalCount     明细笔数
 * @param purpose        用途
 * @param summary        摘要
 * @author navms
 */
public record SubmitBankPayOrderCommand(
        String tenantId,
        String bankPayOrderNo,
        String accountNo,
        BigDecimal totalAmount,
        Integer totalCount,
        String purpose,
        String summary
) {
}
