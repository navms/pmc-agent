package io.github.navms.domain.bank.entity;

import io.github.navms.domain.bank.enums.TradeDirection;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 余额流水。
 *
 * @author navms
 */
@Getter
public class BalanceFlow {

    private Long id;

    private final String tenantId;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private final String flowNo;

    private final String accountNo;

    private String tradeDetailNo;

    private TradeDirection direction;

    private BigDecimal changeAmount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private LocalDateTime occurTime;

    private String bizType;

    private String summary;

    private BalanceFlow(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.flowNo = builder.flowNo;
        this.accountNo = builder.accountNo;
        this.tradeDetailNo = builder.tradeDetailNo;
        this.direction = builder.direction;
        this.changeAmount = builder.changeAmount == null ? BigDecimal.ZERO : builder.changeAmount;
        this.balanceBefore = builder.balanceBefore == null ? BigDecimal.ZERO : builder.balanceBefore;
        this.balanceAfter = builder.balanceAfter == null ? BigDecimal.ZERO : builder.balanceAfter;
        this.occurTime = builder.occurTime;
        this.bizType = builder.bizType;
        this.summary = builder.summary;
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

        private final String flowNo;

        private final String accountNo;

        private String tradeDetailNo;

        private TradeDirection direction;

        private BigDecimal changeAmount;

        private BigDecimal balanceBefore;

        private BigDecimal balanceAfter;

        private LocalDateTime occurTime;

        private String bizType;

        private String summary;

        /**
         * @param tenantId  租户
         * @param flowNo    流水号
         * @param accountNo 账号
         */
        public Builder(String tenantId, String flowNo, String accountNo) {
            if (tenantId == null || tenantId.isBlank()) {
                throw new IllegalArgumentException("tenantId cannot be blank");
            }
            if (flowNo == null || flowNo.isBlank()) {
                throw new IllegalArgumentException("flowNo cannot be blank");
            }
            if (accountNo == null || accountNo.isBlank()) {
                throw new IllegalArgumentException("accountNo cannot be blank");
            }
            this.tenantId = tenantId;
            this.flowNo = flowNo;
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

        public Builder tradeDetailNo(String tradeDetailNo) {
            this.tradeDetailNo = tradeDetailNo;
            return this;
        }

        public Builder direction(TradeDirection direction) {
            this.direction = direction;
            return this;
        }

        public Builder changeAmount(BigDecimal changeAmount) {
            this.changeAmount = changeAmount;
            return this;
        }

        public Builder balanceBefore(BigDecimal balanceBefore) {
            this.balanceBefore = balanceBefore;
            return this;
        }

        public Builder balanceAfter(BigDecimal balanceAfter) {
            this.balanceAfter = balanceAfter;
            return this;
        }

        public Builder occurTime(LocalDateTime occurTime) {
            this.occurTime = occurTime;
            return this;
        }

        public Builder bizType(String bizType) {
            this.bizType = bizType;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        /**
         * @return 余额流水
         */
        public BalanceFlow build() {
            if (direction == null) {
                throw new IllegalArgumentException("direction cannot be null");
            }
            return new BalanceFlow(this);
        }
    }
}
