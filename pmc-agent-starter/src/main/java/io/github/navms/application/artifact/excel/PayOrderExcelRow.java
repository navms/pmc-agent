package io.github.navms.application.artifact.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单 Excel 行。
 *
 * @author navms
 */
@Getter
@Setter
public class PayOrderExcelRow {

    @ExcelProperty("支付单号")
    private String bankPayOrderNo;

    @ExcelProperty("付款账号")
    private String accountNo;

    @ExcelProperty("付款户名")
    private String payerName;

    @ExcelProperty("合计金额")
    private BigDecimal totalAmount;

    @ExcelProperty("明细笔数")
    private Integer totalCount;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("用途")
    private String purpose;

    @ExcelProperty("申请时间")
    private LocalDateTime applyTime;
}
