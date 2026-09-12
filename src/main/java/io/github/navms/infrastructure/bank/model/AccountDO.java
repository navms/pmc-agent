package io.github.navms.infrastructure.bank.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 银行账户表。
 *
 * @author navms
 */
@Data
@TableName("bank_account")
public class AccountDO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String tenantId;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    private String accountNo;

    private String accountName;

    private String bankCode;

    private String bankName;

    private String branchName;

    private String currency;

    /** @see io.github.navms.domain.bank.enums.AccountType */
    private String accountType;

    /** @see io.github.navms.domain.bank.enums.AccountStatus */
    private String status;

    private BigDecimal balance;

    private BigDecimal availableBalance;

    private BigDecimal frozenAmount;

    private LocalDate openDate;

    private String remark;
}
