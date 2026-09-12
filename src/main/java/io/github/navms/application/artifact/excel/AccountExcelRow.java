package io.github.navms.application.artifact.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 账户 Excel 行。
 *
 * @author navms
 */
@Getter
@Setter
public class AccountExcelRow {

    @ExcelProperty("账号")
    private String accountNo;

    @ExcelProperty("户名")
    private String accountName;

    @ExcelProperty("开户行")
    private String bankName;

    @ExcelProperty("账户类型")
    private String accountType;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("当前余额")
    private BigDecimal balance;

    @ExcelProperty("可用余额")
    private BigDecimal availableBalance;
}
