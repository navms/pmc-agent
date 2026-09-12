package io.github.navms.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 交易明细表。
 *
 * @author navms
 */
@Data
@TableName("trade_detail")
public class TradeDetailDO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String tenantId;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    private String tradeDetailNo;

    private String receiptNo;

    private String accountNo;

    /**
     * @see io.github.navms.domain.bank.enums.TradeDirection
     */
    private String direction;

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
}
