package io.github.navms.domain.bank.entity;

import io.github.navms.domain.bank.enums.TradeDirection;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 交易明细。
 *
 * @author navms
 */
@Getter
public class TradeDetail {

    private Long id;

    private final String tenantId;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private final String tradeDetailNo;

    private String receiptNo;

    private final String accountNo;

    private TradeDirection direction;

    private BigDecimal amount;

    private String currency;

    private String counterpartAccountNo;

    private String counterpartName;

    private String counterpartBankName;

    private String summary;

    private LocalDateTime tradeTime;

    private LocalDate valueDate;

    private BigDecimal balanceAfter;

    private String bankSerialNo;

    private TradeDetail(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.tradeDetailNo = builder.tradeDetailNo;
        this.receiptNo = builder.receiptNo;
        this.accountNo = builder.accountNo;
        this.direction = builder.direction;
        this.amount = builder.amount == null ? BigDecimal.ZERO : builder.amount;
        this.currency = builder.currency == null ? "CNY" : builder.currency;
        this.counterpartAccountNo = builder.counterpartAccountNo;
        this.counterpartName = builder.counterpartName;
        this.counterpartBankName = builder.counterpartBankName;
        this.summary = builder.summary;
        this.tradeTime = builder.tradeTime;
        this.valueDate = builder.valueDate;
        this.balanceAfter = builder.balanceAfter == null ? BigDecimal.ZERO : builder.balanceAfter;
        this.bankSerialNo = builder.bankSerialNo;
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

        private final String tradeDetailNo;

        private String receiptNo;

        private final String accountNo;

        private TradeDirection direction;

        private BigDecimal amount;

        private String currency;

        private String counterpartAccountNo;

        private String counterpartName;

        private String counterpartBankName;

        private String summary;

        private LocalDateTime tradeTime;

        private LocalDate valueDate;

        private BigDecimal balanceAfter;

        private String bankSerialNo;

        /**
         * @param tenantId      租户
         * @param tradeDetailNo 交易明细号
         * @param accountNo     账号
         */
        public Builder(String tenantId, String tradeDetailNo, String accountNo) {
            if (tenantId == null || tenantId.isBlank()) {
                throw new IllegalArgumentException("tenantId cannot be blank");
            }
            if (tradeDetailNo == null || tradeDetailNo.isBlank()) {
                throw new IllegalArgumentException("tradeDetailNo cannot be blank");
            }
            if (accountNo == null || accountNo.isBlank()) {
                throw new IllegalArgumentException("accountNo cannot be blank");
            }
            this.tenantId = tenantId;
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

        public Builder receiptNo(String receiptNo) {
            this.receiptNo = receiptNo;
            return this;
        }

        public Builder direction(TradeDirection direction) {
            this.direction = direction;
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

        public Builder counterpartAccountNo(String counterpartAccountNo) {
            this.counterpartAccountNo = counterpartAccountNo;
            return this;
        }

        public Builder counterpartName(String counterpartName) {
            this.counterpartName = counterpartName;
            return this;
        }

        public Builder counterpartBankName(String counterpartBankName) {
            this.counterpartBankName = counterpartBankName;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder tradeTime(LocalDateTime tradeTime) {
            this.tradeTime = tradeTime;
            return this;
        }

        public Builder valueDate(LocalDate valueDate) {
            this.valueDate = valueDate;
            return this;
        }

        public Builder balanceAfter(BigDecimal balanceAfter) {
            this.balanceAfter = balanceAfter;
            return this;
        }

        public Builder bankSerialNo(String bankSerialNo) {
            this.bankSerialNo = bankSerialNo;
            return this;
        }

        /**
         * @return 交易明细
         */
        public TradeDetail build() {
            if (direction == null) {
                throw new IllegalArgumentException("direction cannot be null");
            }
            return new TradeDetail(this);
        }
    }
}
