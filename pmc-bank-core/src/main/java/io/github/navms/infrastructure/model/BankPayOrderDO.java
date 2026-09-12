package io.github.navms.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 银行支付单表。
 *
 * @author navms
 */
@Data
@TableName("bank_pay_order")
public class BankPayOrderDO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String tenantId;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    private String bankPayOrderNo;

    private String accountNo;

    private String payerName;

    private BigDecimal totalAmount;

    private Integer totalCount;

    private String currency;

    /**
     * @see io.github.navms.domain.bank.enums.PayOrderStatus
     */
    private String status;

    private String purpose;

    private String summary;

    private LocalDateTime applyTime;

    private LocalDateTime payTime;

    private String channel;

    private String failReason;
}
