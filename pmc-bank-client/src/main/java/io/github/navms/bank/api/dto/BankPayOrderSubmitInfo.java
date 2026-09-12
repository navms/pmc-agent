package io.github.navms.bank.api.dto;

import java.math.BigDecimal;

/**
 * 支付单提交结果。
 *
 * @param mocked         是否 mock
 * @param tenantId       租户 ID
 * @param bankPayOrderNo 支付单号
 * @param accountNo      付款账号
 * @param totalAmount    合计金额
 * @param totalCount     明细笔数
 * @param purpose        用途
 * @param summary        摘要
 * @param status         状态
 * @param bankAcceptNo   银行受理号
 * @author navms
 */
public record BankPayOrderSubmitInfo(
        boolean mocked,
        String tenantId,
        String bankPayOrderNo,
        String accountNo,
        BigDecimal totalAmount,
        Integer totalCount,
        String purpose,
        String summary,
        String status,
        String bankAcceptNo
) {
}
