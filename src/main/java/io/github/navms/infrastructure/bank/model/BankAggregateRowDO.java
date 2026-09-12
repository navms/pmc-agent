package io.github.navms.infrastructure.bank.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 聚合查询行。
 *
 * @author navms
 */
@Data
public class BankAggregateRowDO {

    private String bucket;

    private Long rowCount;

    private BigDecimal totalAmount;
}
