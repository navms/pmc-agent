package io.github.navms.domain.bank.entity;

import io.github.navms.domain.bank.enums.PayOrderStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 银行支付明细单。
 *
 * @author navms
 */
@Getter
public class BankPayOrderDetail {

    private Long id;

    private final String tenantId;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private final Long bankPayOrderId;

    private final String bankPayOrderNo;

    private final String bankPayOrderDetailNo;

    private final String accountNo;

    private String payeeAccountNo;

    private String payeeName;

    private String payeeBankName;

    private BigDecimal amount;

    private String currency;

    private PayOrderStatus status;

    private String usage;

    private Integer seqNo;

    private String failReason;

    private BankPayOrderDetail(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.bankPayOrderId = builder.bankPayOrderId;
        this.bankPayOrderNo = builder.bankPayOrderNo;
        this.bankPayOrderDetailNo = builder.bankPayOrderDetailNo;
        this.accountNo = builder.accountNo;
        this.payeeAccountNo = builder.payeeAccountNo;
        this.payeeName = builder.payeeName;
        this.payeeBankName = builder.payeeBankName;
        this.amount = builder.amount == null ? BigDecimal.ZERO : builder.amount;
        this.currency = builder.currency == null ? "CNY" : builder.currency;
        this.status = builder.status == null ? PayOrderStatus.SUBMITTED : builder.status;
        this.usage = builder.usage;
        this.seqNo = builder.seqNo == null ? 1 : builder.seqNo;
        this.failReason = builder.failReason;
    }

    /**
     * 明细号即交易明细号。
     *
     * @return 交易明细号
     */
    public String tradeDetailNo() {
        return bankPayOrderDetailNo;
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

        private final Long bankPayOrderId;

        private final String bankPayOrderNo;

        private final String bankPayOrderDetailNo;

        private final String accountNo;

        private String payeeAccountNo;

        private String payeeName;

        private String payeeBankName;

        private BigDecimal amount;

        private String currency;

        private PayOrderStatus status;

        private String usage;

        private Integer seqNo;

        private String failReason;

        /**
         * @param tenantId             租户
         * @param bankPayOrderId       支付单主键
         * @param bankPayOrderNo       支付单号
         * @param bankPayOrderDetailNo 明细号
         * @param accountNo            付款账号
         */
        public Builder(
                String tenantId,
                Long bankPayOrderId,
                String bankPayOrderNo,
                String bankPayOrderDetailNo,
                String accountNo) {
            if (tenantId == null || tenantId.isBlank()) {
                throw new IllegalArgumentException("tenantId cannot be blank");
            }
            if (bankPayOrderId == null) {
                throw new IllegalArgumentException("bankPayOrderId cannot be null");
            }
            if (bankPayOrderNo == null || bankPayOrderNo.isBlank()) {
                throw new IllegalArgumentException("bankPayOrderNo cannot be blank");
            }
            if (bankPayOrderDetailNo == null || bankPayOrderDetailNo.isBlank()) {
                throw new IllegalArgumentException("bankPayOrderDetailNo cannot be blank");
            }
            if (accountNo == null || accountNo.isBlank()) {
                throw new IllegalArgumentException("accountNo cannot be blank");
            }
            this.tenantId = tenantId;
            this.bankPayOrderId = bankPayOrderId;
            this.bankPayOrderNo = bankPayOrderNo;
            this.bankPayOrderDetailNo = bankPayOrderDetailNo;
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

        public Builder payeeAccountNo(String payeeAccountNo) {
            this.payeeAccountNo = payeeAccountNo;
            return this;
        }

        public Builder payeeName(String payeeName) {
            this.payeeName = payeeName;
            return this;
        }

        public Builder payeeBankName(String payeeBankName) {
            this.payeeBankName = payeeBankName;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
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

        public Builder usage(String usage) {
            this.usage = usage;
            return this;
        }

        public Builder seqNo(Integer seqNo) {
            this.seqNo = seqNo;
            return this;
        }

        public Builder failReason(String failReason) {
            this.failReason = failReason;
            return this;
        }

        /**
         * @return 明细
         */
        public BankPayOrderDetail build() {
            return new BankPayOrderDetail(this);
        }
    }
}
