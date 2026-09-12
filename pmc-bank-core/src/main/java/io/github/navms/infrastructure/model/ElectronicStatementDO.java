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
 * 电子对账单表。
 *
 * @author navms
 */
@Data
@TableName("electronic_statement")
public class ElectronicStatementDO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String tenantId;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    private String statementNo;

    private String tradeDetailNo;

    private String accountNo;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private BigDecimal openingBalance;

    private BigDecimal closingBalance;

    private BigDecimal debitTotal;

    private BigDecimal creditTotal;

    /**
     * @see io.github.navms.domain.bank.enums.StatementStatus
     */
    private String status;

    private LocalDateTime issueTime;
}
