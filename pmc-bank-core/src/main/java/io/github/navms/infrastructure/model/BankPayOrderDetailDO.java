package io.github.navms.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 银行支付明细表。
 *
 * @author navms
 */
@Data
@TableName("bank_pay_order_detail")
public class BankPayOrderDetailDO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String tenantId;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    private Long bankPayOrderId;

    private String bankPayOrderNo;

    private String bankPayOrderDetailNo;

    private String accountNo;

    private String payeeAccountNo;

    private String payeeName;

    private String payeeBankName;

    private BigDecimal amount;

    private String currency;

    /**
     * @see io.github.navms.domain.bank.enums.PayOrderStatus
     */
    private String status;

    @TableField("usage_desc")
    private String usageDesc;

    private Integer seqNo;

    private String failReason;
}
