package io.github.navms.bank.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 银行支付单信息。
 *
 * @param tenantId       租户 ID
 * @param createdBy      创建人 ID
 * @param id             主键
 * @param bankPayOrderNo 支付单号
 * @param accountNo      付款账号
 * @param payerName      付款户名
 * @param totalAmount    合计金额
 * @param totalCount     明细笔数
 * @param currency       币种
 * @param status         状态
 * @param purpose        用途
 * @param summary        摘要
 * @param applyTime      申请时间
 * @param payTime        支付时间
 * @param channel        渠道
 * @param failReason     失败原因
 * @param createdAt      创建时间
 * @author navms
 */
public record BankPayOrderInfo(
        String tenantId,
        String createdBy,
        Long id,
        String bankPayOrderNo,
        String accountNo,
        String payerName,
        BigDecimal totalAmount,
        Integer totalCount,
        String currency,
        String status,
        String purpose,
        String summary,
        LocalDateTime applyTime,
        LocalDateTime payTime,
        String channel,
        String failReason,
        LocalDateTime createdAt
) {
}
