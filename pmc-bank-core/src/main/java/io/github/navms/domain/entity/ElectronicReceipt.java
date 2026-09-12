package io.github.navms.domain.bank.entity;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 电子回单。
 *
 * @author navms
 */
@Getter
public class ElectronicReceipt {

    private Long id;

    private final String tenantId;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private final String receiptNo;

    private final String tradeDetailNo;

    private final String accountNo;

    private BigDecimal amount;

    private String currency;

    private String payerName;

    private String payeeName;

    private LocalDateTime issueTime;

    private String bankName;

    private String digest;

    private ElectronicReceipt(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.receiptNo = builder.receiptNo;
        this.tradeDetailNo = builder.tradeDetailNo;
        this.accountNo = builder.accountNo;
        this.amount = builder.amount == null ? BigDecimal.ZERO : builder.amount;
        this.currency = builder.currency == null ? "CNY" : builder.currency;
        this.payerName = builder.payerName;
        this.payeeName = builder.payeeName;
        this.issueTime = builder.issueTime;
        this.bankName = builder.bankName;
        this.digest = builder.digest;
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

        private final String receiptNo;

        private final String tradeDetailNo;

        private final String accountNo;

        private BigDecimal amount;

        private String currency;

        private String payerName;

        private String payeeName;

        private LocalDateTime issueTime;

        private String bankName;

        private String digest;

        /**
         * @param tenantId      租户
         * @param receiptNo     回单号
         * @param tradeDetailNo 交易明细号
         * @param accountNo     账号
         */
        public Builder(String tenantId, String receiptNo, String tradeDetailNo, String accountNo) {
            if (tenantId == null || tenantId.isBlank()) {
                throw new IllegalArgumentException("tenantId cannot be blank");
            }
            if (receiptNo == null || receiptNo.isBlank()) {
                throw new IllegalArgumentException("receiptNo cannot be blank");
            }
            if (tradeDetailNo == null || tradeDetailNo.isBlank()) {
                throw new IllegalArgumentException("tradeDetailNo cannot be blank");
            }
            if (accountNo == null || accountNo.isBlank()) {
                throw new IllegalArgumentException("accountNo cannot be blank");
            }
            this.tenantId = tenantId;
            this.receiptNo = receiptNo;
            this.tradeDetailNo = tradeDetailNo;
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

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder payerName(String payerName) {
            this.payerName = payerName;
            return this;
        }

        public Builder payeeName(String payeeName) {
            this.payeeName = payeeName;
            return this;
        }

        public Builder issueTime(LocalDateTime issueTime) {
            this.issueTime = issueTime;
            return this;
        }

        public Builder bankName(String bankName) {
            this.bankName = bankName;
            return this;
        }

        public Builder digest(String digest) {
            this.digest = digest;
            return this;
        }

        /**
         * @return 电子回单
         */
        public ElectronicReceipt build() {
            return new ElectronicReceipt(this);
        }
    }
}
