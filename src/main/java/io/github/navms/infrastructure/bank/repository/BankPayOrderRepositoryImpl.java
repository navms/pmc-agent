package io.github.navms.infrastructure.bank.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.navms.domain.bank.entity.BankPayOrder;
import io.github.navms.domain.bank.enums.AggregateGroup;
import io.github.navms.domain.bank.enums.PayOrderStatus;
import io.github.navms.domain.bank.repository.BankPayOrderRepository;
import io.github.navms.domain.bank.valueobj.BankAggregateRow;
import io.github.navms.infrastructure.bank.converter.BankPayOrderConverter;
import io.github.navms.infrastructure.bank.mapper.BankPayOrderMapper;
import io.github.navms.infrastructure.bank.model.BankAggregateRowDO;
import io.github.navms.infrastructure.bank.model.BankPayOrderDO;
import io.github.navms.infrastructure.bank.support.AggregateSql;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 银行支付单仓储。
 *
 * @author navms
 */
@Repository
@RequiredArgsConstructor
public class BankPayOrderRepositoryImpl implements BankPayOrderRepository {

    private final BankPayOrderMapper bankPayOrderMapper;

    @Override
    public List<BankPayOrder> query(
            String tenantId,
            String bankPayOrderNo,
            String accountNo,
            PayOrderStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int maxRows) {
        LambdaQueryWrapper<BankPayOrderDO> wrapper = new LambdaQueryWrapper<BankPayOrderDO>()
                .eq(BankPayOrderDO::getTenantId, tenantId)
                .eq(StringUtils.hasText(bankPayOrderNo), BankPayOrderDO::getBankPayOrderNo, bankPayOrderNo)
                .eq(StringUtils.hasText(accountNo), BankPayOrderDO::getAccountNo, accountNo)
                .eq(status != null, BankPayOrderDO::getStatus, status == null ? null : status.getCode())
                .ge(startDate != null, BankPayOrderDO::getApplyTime, startDate == null ? null : startDate.atStartOfDay())
                .lt(endDate != null, BankPayOrderDO::getApplyTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .orderByDesc(BankPayOrderDO::getApplyTime)
                .last("LIMIT " + Math.max(maxRows, 0));
        return BankPayOrderConverter.INSTANCE.toBankPayOrderList(bankPayOrderMapper.selectList(wrapper));
    }

    @Override
    public List<BankAggregateRow> aggregate(
            String tenantId,
            String accountNo,
            PayOrderStatus status,
            LocalDate startDate,
            LocalDate endDate,
            AggregateGroup group,
            int maxRows) {
        List<BankAggregateRowDO> rows = bankPayOrderMapper.aggregate(
                tenantId,
                StringUtils.hasText(accountNo) ? accountNo : null,
                status == null ? null : status.getCode(),
                startDate == null ? null : startDate.atStartOfDay(),
                endDate == null ? null : endDate.plusDays(1).atStartOfDay(),
                AggregateSql.payOrder(group),
                Math.max(maxRows, 0));
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
