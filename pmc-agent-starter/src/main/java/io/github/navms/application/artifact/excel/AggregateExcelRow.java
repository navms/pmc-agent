package io.github.navms.application.artifact.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 汇总 Excel 行。
 *
 * @author navms
 */
@Getter
@Setter
public class AggregateExcelRow {

    @ExcelProperty("分组")
    private String bucket;

    @ExcelProperty("笔数")
    private Long rowCount;

    @ExcelProperty("金额合计")
    private BigDecimal totalAmount;
}
