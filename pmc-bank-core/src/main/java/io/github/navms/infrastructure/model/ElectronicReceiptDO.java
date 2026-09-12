package io.github.navms.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 电子回单表。
 *
 * @author navms
 */
@Data
@TableName("electronic_receipt")
public class ElectronicReceiptDO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String tenantId;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    private String receiptNo;

    private String tradeDetailNo;

    private String accountNo;

    private BigDecimal amount;

    private String currency;

    private String payerName;

    private String payeeName;

    private LocalDateTime issueTime;

    private String bankName;

    private String digest;
}
