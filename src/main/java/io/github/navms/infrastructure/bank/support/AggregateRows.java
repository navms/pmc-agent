package io.github.navms.infrastructure.bank.support;

import io.github.navms.domain.bank.valueobj.BankAggregateRow;
import io.github.navms.infrastructure.bank.model.BankAggregateRowDO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 聚合 DO 转领域行。
 *
 * @author navms
 */
public final class AggregateRows {

    private AggregateRows() {
    }

    /**
     * @param rows 聚合 DO
     * @return 领域行
     */
    public static List<BankAggregateRow> toDomain(List<BankAggregateRowDO> rows) {
        List<BankAggregateRow> result = new ArrayList<>(rows.size());
        for (BankAggregateRowDO row : rows) {
            result.add(new BankAggregateRow(
                    row.getBucket(),
                    row.getRowCount() == null ? 0L : row.getRowCount(),
                    row.getTotalAmount() == null ? BigDecimal.ZERO : row.getTotalAmount()));
        }
        return result;
    }
}
