package io.github.navms.infrastructure.bank.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.navms.domain.bank.BankDefaults;
import io.github.navms.domain.bank.entity.BankPayOrderDetail;
import io.github.navms.domain.bank.enums.AggregateGroup;
import io.github.navms.domain.bank.enums.PayOrderStatus;
import io.github.navms.domain.bank.repository.BankPayOrderDetailRepository;
import io.github.navms.domain.bank.valueobj.BankAggregateRow;
import io.github.navms.infrastructure.bank.converter.BankPayOrderDetailConverter;
import io.github.navms.infrastructure.bank.mapper.BankPayOrderDetailMapper;
import io.github.navms.infrastructure.bank.model.BankPayOrderDetailDO;
import io.github.navms.infrastructure.bank.support.AggregateRows;
import io.github.navms.infrastructure.bank.support.AggregateSql;
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
    public List<BankPayOrderDetail> query(
            String tenantId,
            String bankPayOrderNo,
            String bankPayOrderDetailNo,
            String accountNo,
            PayOrderStatus status) {
        LambdaQueryWrapper<BankPayOrderDetailDO> wrapper = new LambdaQueryWrapper<BankPayOrderDetailDO>()
                .eq(BankPayOrderDetailDO::getTenantId, tenantId)
                .eq(StringUtils.hasText(bankPayOrderNo), BankPayOrderDetailDO::getBankPayOrderNo, bankPayOrderNo)
                .eq(StringUtils.hasText(bankPayOrderDetailNo), BankPayOrderDetailDO::getBankPayOrderDetailNo, bankPayOrderDetailNo)
                .eq(StringUtils.hasText(accountNo), BankPayOrderDetailDO::getAccountNo, accountNo)
                .eq(status != null, BankPayOrderDetailDO::getStatus, status == null ? null : status.getCode())
                .orderByAsc(BankPayOrderDetailDO::getBankPayOrderNo)
                .orderByAsc(BankPayOrderDetailDO::getSeqNo)
                .last("LIMIT " + BankDefaults.MAX_QUERY_ROWS);
        return BankPayOrderDetailConverter.INSTANCE.toBankPayOrderDetailList(
                bankPayOrderDetailMapper.selectList(wrapper));
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
        return AggregateRows.toDomain(bankPayOrderDetailMapper.aggregate(
                tenantId,
                StringUtils.hasText(accountNo) ? accountNo : null,
                status == null ? null : status.getCode(),
                startDate == null ? null : startDate.atStartOfDay(),
                endDate == null ? null : endDate.plusDays(1).atStartOfDay(),
                AggregateSql.payOrderDetail(group),
                Math.max(maxRows, 0)));
    }
}
