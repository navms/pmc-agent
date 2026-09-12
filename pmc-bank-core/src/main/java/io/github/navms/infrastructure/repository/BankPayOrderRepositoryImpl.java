package io.github.navms.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.navms.domain.bank.entity.BankPayOrder;
import io.github.navms.domain.bank.enums.PayOrderStatus;
import io.github.navms.domain.bank.repository.BankPayOrderRepository;
import io.github.navms.domain.bank.valueobj.AggregateRow;
import io.github.navms.infrastructure.converter.AggregateRowConverter;
import io.github.navms.infrastructure.converter.BankPayOrderConverter;
import io.github.navms.infrastructure.mapper.BankPayOrderMapper;
import io.github.navms.infrastructure.model.AggregateRowDO;
import io.github.navms.infrastructure.model.BankPayOrderDO;
import io.github.navms.infrastructure.support.AggregateSql;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
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
    public List<BankPayOrder> query(String tenantId, String bankPayOrderNo, String accountNo,
                                    PayOrderStatus status, LocalDate startDate, LocalDate endDate, int maxRows) {
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
    public List<AggregateRow> aggregate(String tenantId, String accountNo, PayOrderStatus status,
                                        LocalDate startDate, LocalDate endDate, AggregateSql aggregateSql, int maxRows) {
        List<AggregateRowDO> rows = bankPayOrderMapper.aggregate(tenantId,
                StringUtils.hasText(accountNo) ? accountNo : null, status == null ? null : status.getCode(),
                startDate == null ? null : startDate.atStartOfDay(), endDate == null ? null : endDate.plusDays(1).atStartOfDay(),
                aggregateSql.getAggregateSql(), Math.max(maxRows, 0));
        return AggregateRowConverter.INSTANCE.toAggregateRowList(rows);
    }
}
