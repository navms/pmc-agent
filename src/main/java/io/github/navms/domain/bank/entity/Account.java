package io.github.navms.domain.bank.entity;

import io.github.navms.domain.bank.enums.AccountStatus;
import io.github.navms.domain.bank.enums.AccountType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 银行账户。
 *
 * @author navms
 */
@Getter
public class Account {

    private Long id;

    private final String tenantId;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private final String accountNo;

    private String accountName;

    private String bankCode;

    private String bankName;

    private String branchName;

    private String currency;

    private AccountType accountType;

    private AccountStatus status;

    private BigDecimal balance;

    private BigDecimal availableBalance;

    private BigDecimal frozenAmount;

    private LocalDate openDate;

    private String remark;

    private Account(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.accountNo = builder.accountNo;
        this.accountName = builder.accountName;
        this.bankCode = builder.bankCode;
        this.bankName = builder.bankName;
        this.branchName = builder.branchName;
        this.currency = builder.currency == null ? "CNY" : builder.currency;
        this.accountType = builder.accountType;
        this.status = builder.status == null ? AccountStatus.ACTIVE : builder.status;
        this.balance = nvl(builder.balance);
        this.availableBalance = nvl(builder.availableBalance);
        this.frozenAmount = nvl(builder.frozenAmount);
        this.openDate = builder.openDate;
        this.remark = builder.remark;
    }

    /**
     * 冻结账户。
     */
    public void freeze() {
        this.status = AccountStatus.FROZEN;
        touch();
    }

    /**
     * 恢复正常。
     */
    public void activate() {
        this.status = AccountStatus.ACTIVE;
        touch();
    }

    private void touch() {
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

        private final String accountNo;

        private String accountName;

        private String bankCode;

        private String bankName;

        private String branchName;

        private String currency;

        private AccountType accountType;

        private AccountStatus status;

        private BigDecimal balance;

        private BigDecimal availableBalance;

        private BigDecimal frozenAmount;

        private LocalDate openDate;

        private String remark;

        /**
         * @param tenantId  租户
         * @param accountNo 账号
         */
        public Builder(String tenantId, String accountNo) {
            if (tenantId == null || tenantId.isBlank()) {
                throw new IllegalArgumentException("tenantId cannot be blank");
            }
            if (accountNo == null || accountNo.isBlank()) {
                throw new IllegalArgumentException("accountNo cannot be blank");
            }
            this.tenantId = tenantId;
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

        public Builder accountName(String accountName) {
            this.accountName = accountName;
            return this;
        }

        public Builder bankCode(String bankCode) {
            this.bankCode = bankCode;
            return this;
        }

        public Builder bankName(String bankName) {
            this.bankName = bankName;
            return this;
        }

        public Builder branchName(String branchName) {
            this.branchName = branchName;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder accountType(AccountType accountType) {
            this.accountType = accountType;
            return this;
        }

        public Builder status(AccountStatus status) {
            this.status = status;
            return this;
        }

        public Builder balance(BigDecimal balance) {
            this.balance = balance;
            return this;
        }

        public Builder availableBalance(BigDecimal availableBalance) {
            this.availableBalance = availableBalance;
            return this;
        }

        public Builder frozenAmount(BigDecimal frozenAmount) {
            this.frozenAmount = frozenAmount;
            return this;
        }

        public Builder openDate(LocalDate openDate) {
            this.openDate = openDate;
            return this;
        }

        public Builder remark(String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * @return 账户
         */
        public Account build() {
            if (accountType == null) {
                throw new IllegalArgumentException("accountType cannot be null");
            }
            return new Account(this);
        }
    }
}
