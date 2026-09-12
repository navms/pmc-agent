package io.github.navms.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.navms.domain.bank.entity.BankPayOrderDetail;
import io.github.navms.domain.bank.enums.PayOrderStatus;
import io.github.navms.domain.bank.repository.BankPayOrderDetailRepository;
import io.github.navms.domain.bank.valueobj.AggregateRow;
import io.github.navms.infrastructure.converter.AggregateRowConverter;
import io.github.navms.infrastructure.converter.BankPayOrderDetailConverter;
import io.github.navms.infrastructure.mapper.BankPayOrderDetailMapper;
import io.github.navms.infrastructure.model.AggregateRowDO;
import io.github.navms.infrastructure.model.BankPayOrderDetailDO;
import io.github.navms.infrastructure.support.AggregateSql;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 银行支付明细仓储。
 *
 * @author navms
 */
@Repository
@RequiredArgsConstructor
public class BankPayOrderDetailRepositoryImpl implements BankPayOrderDetailRepository {

    private final BankPayOrderDetailMapper bankPayOrderDetailMapper;

    @Override
    public List<BankPayOrderDetail> query(String tenantId, String bankPayOrderNo,
                                          String bankPayOrderDetailNo, String accountNo, PayOrderStatus status, int maxRows) {
        LambdaQueryWrapper<BankPayOrderDetailDO> wrapper = new LambdaQueryWrapper<BankPayOrderDetailDO>()
                .eq(BankPayOrderDetailDO::getTenantId, tenantId)
                .eq(StringUtils.hasText(bankPayOrderNo), BankPayOrderDetailDO::getBankPayOrderNo, bankPayOrderNo)
                .eq(StringUtils.hasText(bankPayOrderDetailNo), BankPayOrderDetailDO::getBankPayOrderDetailNo, bankPayOrderDetailNo)
                .eq(StringUtils.hasText(accountNo), BankPayOrderDetailDO::getAccountNo, accountNo)
                .eq(status != null, BankPayOrderDetailDO::getStatus, status == null ? null : status.getCode())
                .orderByAsc(BankPayOrderDetailDO::getBankPayOrderNo)
                .orderByAsc(BankPayOrderDetailDO::getSeqNo)
                .last("LIMIT " + Math.max(maxRows, 0));
        return BankPayOrderDetailConverter.INSTANCE.toBankPayOrderDetailList(bankPayOrderDetailMapper.selectList(wrapper));
    }

    @Override
    public List<AggregateRow> aggregate(String tenantId, String accountNo, PayOrderStatus status,
                                        LocalDate startDate, LocalDate endDate, AggregateSql aggregateSql, int maxRows) {
        List<AggregateRowDO> rows = bankPayOrderDetailMapper.aggregate(tenantId,
                StringUtils.hasText(accountNo) ? accountNo : null, status == null ? null : status.getCode(),
                startDate == null ? null : startDate.atStartOfDay(), endDate == null ? null : endDate.plusDays(1).atStartOfDay(),
                aggregateSql.getAggregateSql(), Math.max(maxRows, 0));
        return AggregateRowConverter.INSTANCE.toAggregateRowList(rows);
    }
}
