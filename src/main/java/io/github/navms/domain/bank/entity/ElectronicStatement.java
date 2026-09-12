package io.github.navms.domain.bank.entity;

import io.github.navms.domain.bank.enums.StatementStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 电子对账单。
 *
 * @author navms
 */
@Getter
public class ElectronicStatement {

    private Long id;

    private final String tenantId;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private final String statementNo;

    private final String tradeDetailNo;

    private final String accountNo;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private BigDecimal openingBalance;

    private BigDecimal closingBalance;

    private BigDecimal debitTotal;

    private BigDecimal creditTotal;

    private StatementStatus status;

    private LocalDateTime issueTime;

    private ElectronicStatement(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.statementNo = builder.statementNo;
        this.tradeDetailNo = builder.tradeDetailNo;
        this.accountNo = builder.accountNo;
        this.periodStart = builder.periodStart;
        this.periodEnd = builder.periodEnd;
        this.openingBalance = nvl(builder.openingBalance);
        this.closingBalance = nvl(builder.closingBalance);
        this.debitTotal = nvl(builder.debitTotal);
        this.creditTotal = nvl(builder.creditTotal);
        this.status = builder.status == null ? StatementStatus.ISSUED : builder.status;
        this.issueTime = builder.issueTime;
    }

    /**
     * 确认对账单。
     */
    public void confirm() {
        this.status = StatementStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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

        private final String statementNo;

        private final String tradeDetailNo;

        private final String accountNo;

        private LocalDate periodStart;

        private LocalDate periodEnd;

        private BigDecimal openingBalance;

        private BigDecimal closingBalance;

        private BigDecimal debitTotal;

        private BigDecimal creditTotal;

        private StatementStatus status;

        private LocalDateTime issueTime;

        /**
         * @param tenantId      租户
         * @param statementNo   对账单号
         * @param tradeDetailNo 交易明细号
         * @param accountNo     账号
         */
        public Builder(String tenantId, String statementNo, String tradeDetailNo, String accountNo) {
            if (tenantId == null || tenantId.isBlank()) {
                throw new IllegalArgumentException("tenantId cannot be blank");
            }
            if (statementNo == null || statementNo.isBlank()) {
                throw new IllegalArgumentException("statementNo cannot be blank");
            }
            if (tradeDetailNo == null || tradeDetailNo.isBlank()) {
                throw new IllegalArgumentException("tradeDetailNo cannot be blank");
            }
            if (accountNo == null || accountNo.isBlank()) {
                throw new IllegalArgumentException("accountNo cannot be blank");
            }
            this.tenantId = tenantId;
            this.statementNo = statementNo;
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

        public Builder periodStart(LocalDate periodStart) {
            this.periodStart = periodStart;
            return this;
        }

        public Builder periodEnd(LocalDate periodEnd) {
            this.periodEnd = periodEnd;
            return this;
        }

        public Builder openingBalance(BigDecimal openingBalance) {
            this.openingBalance = openingBalance;
            return this;
        }

        public Builder closingBalance(BigDecimal closingBalance) {
            this.closingBalance = closingBalance;
            return this;
        }

        public Builder debitTotal(BigDecimal debitTotal) {
            this.debitTotal = debitTotal;
            return this;
        }

        public Builder creditTotal(BigDecimal creditTotal) {
            this.creditTotal = creditTotal;
            return this;
        }

        public Builder status(StatementStatus status) {
            this.status = status;
            return this;
        }

        public Builder issueTime(LocalDateTime issueTime) {
            this.issueTime = issueTime;
            return this;
        }

        /**
         * @return 电子对账单
         */
        public ElectronicStatement build() {
            return new ElectronicStatement(this);
        }
    }
}
