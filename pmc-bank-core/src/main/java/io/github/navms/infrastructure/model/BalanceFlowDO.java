package io.github.navms.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 余额流水表。
 *
 * @author navms
 */
@Data
@TableName("balance_flow")
public class BalanceFlowDO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String tenantId;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    private String flowNo;

    private String accountNo;

    private String tradeDetailNo;

    /**
     * @see io.github.navms.domain.bank.enums.TradeDirection
     */
    private String direction;

    private BigDecimal changeAmount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private LocalDateTime occurTime;

    private String bizType;

    private String summary;
}
