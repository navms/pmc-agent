package io.github.navms.application.artifact.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易明细 Excel 行。
 *
 * @author navms
 */
@Getter
@Setter
public class TradeDetailExcelRow {

    @ExcelProperty("交易明细号")
    private String tradeDetailNo;

    @ExcelProperty("账号")
    private String accountNo;

    @ExcelProperty("借贷方向")
    private String direction;

    @ExcelProperty("金额")
    private BigDecimal amount;

    @ExcelProperty("币种")
    private String currency;

    @ExcelProperty("对方户名")
    private String counterpartName;

    @ExcelProperty("摘要")
    private String summary;

    @ExcelProperty("交易时间")
    private LocalDateTime tradeTime;

    @ExcelProperty("交易后余额")
    private BigDecimal balanceAfter;
}
