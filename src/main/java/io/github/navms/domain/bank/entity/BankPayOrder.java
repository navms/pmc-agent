package io.github.navms.domain.bank.entity;

import io.github.navms.domain.bank.enums.PayOrderStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 银行支付单。
 *
 * @author navms
 */
@Getter
public class BankPayOrder {

    private Long id;

    private final String tenantId;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private final String bankPayOrderNo;

    private final String accountNo;

    private String payerName;

    private BigDecimal totalAmount;

    private Integer totalCount;

    private String currency;

    private PayOrderStatus status;

    private String purpose;

    private String summary;

    private LocalDateTime applyTime;

    private LocalDateTime payTime;

    private String channel;

    private String failReason;

    private BankPayOrder(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.bankPayOrderNo = builder.bankPayOrderNo;
        this.accountNo = builder.accountNo;
        this.payerName = builder.payerName;
        this.totalAmount = builder.totalAmount == null ? BigDecimal.ZERO : builder.totalAmount;
        this.totalCount = builder.totalCount == null ? 0 : builder.totalCount;
        this.currency = builder.currency == null ? "CNY" : builder.currency;
        this.status = builder.status == null ? PayOrderStatus.SUBMITTED : builder.status;
        this.purpose = builder.purpose;
        this.summary = builder.summary;
        this.applyTime = builder.applyTime;
        this.payTime = builder.payTime;
        this.channel = builder.channel;
        this.failReason = builder.failReason;
    }

    /**
     * 标记支付成功。
     *
     * @param payTime 支付时间
     */
    public void markSuccess(LocalDateTime payTime) {
        this.status = PayOrderStatus.SUCCESS;
        this.payTime = payTime;
        this.failReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记支付失败。
     *
     * @param reason 原因
     */
    public void markFailed(String reason) {
        this.status = PayOrderStatus.FAILED;
        this.failReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * @author navms
     */
    public static final class Builder {

        private Long id;

        private final String tenantId;

        private String createdBy;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;

        private final String bankPayOrderNo;

        private final String accountNo;

        private String payerName;

        private BigDecimal totalAmount;

        private Integer totalCount;

        private String currency;

        private PayOrderStatus status;

        private String purpose;

        private String summary;

        private LocalDateTime applyTime;

        private LocalDateTime payTime;

        private String channel;

        private String failReason;

        /**
         * @param tenantId       租户
         * @param bankPayOrderNo 支付单号
         * @param accountNo      付款账号
         */
        public Builder(String tenantId, String bankPayOrderNo, String accountNo) {
            if (tenantId == null || tenantId.isBlank()) {
                throw new IllegalArgumentException("tenantId cannot be blank");
            }
            if (bankPayOrderNo == null || bankPayOrderNo.isBlank()) {
                throw new IllegalArgumentException("bankPayOrderNo cannot be blank");
            }
            if (accountNo == null || accountNo.isBlank()) {
                throw new IllegalArgumentException("accountNo cannot be blank");
            }
            this.tenantId = tenantId;
            this.bankPayOrderNo = bankPayOrderNo;
            this.accountNo = accountNo;
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder payerName(String payerName) {
            this.payerName = payerName;
            return this;
        }

        public Builder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder totalCount(Integer totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder status(PayOrderStatus status) {
            this.status = status;
            return this;
        }

        public Builder purpose(String purpose) {
            this.purpose = purpose;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder applyTime(LocalDateTime applyTime) {
            this.applyTime = applyTime;
            return this;
        }

        public Builder payTime(LocalDateTime payTime) {
            this.payTime = payTime;
            return this;
        }

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public Builder failReason(String failReason) {
            this.failReason = failReason;
            return this;
        }

        /**
         * @return 支付单
         */
        public BankPayOrder build() {
            return new BankPayOrder(this);
        }
    }
}
