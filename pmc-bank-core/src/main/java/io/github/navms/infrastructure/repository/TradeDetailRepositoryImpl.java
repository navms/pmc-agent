package io.github.navms.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.navms.domain.bank.entity.TradeDetail;
import io.github.navms.domain.bank.enums.TradeDirection;
import io.github.navms.domain.bank.repository.TradeDetailRepository;
import io.github.navms.domain.bank.valueobj.AggregateRow;
import io.github.navms.infrastructure.converter.AggregateRowConverter;
import io.github.navms.infrastructure.converter.TradeDetailConverter;
import io.github.navms.infrastructure.mapper.TradeDetailMapper;
import io.github.navms.infrastructure.model.AggregateRowDO;
import io.github.navms.infrastructure.model.TradeDetailDO;
import io.github.navms.infrastructure.support.AggregateSql;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 交易明细仓储。
 *
 * @author navms
 */
@Repository
@RequiredArgsConstructor
public class TradeDetailRepositoryImpl implements TradeDetailRepository {

    private final TradeDetailMapper tradeDetailMapper;

    @Override
    public List<TradeDetail> query(String tenantId, String accountNo, String tradeDetailNo, String receiptNo,
                                   TradeDirection direction, LocalDate startDate, LocalDate endDate, int maxRows) {
        LambdaQueryWrapper<TradeDetailDO> wrapper = new LambdaQueryWrapper<TradeDetailDO>()
                .eq(TradeDetailDO::getTenantId, tenantId)
                .eq(StringUtils.hasText(accountNo), TradeDetailDO::getAccountNo, accountNo)
                .eq(StringUtils.hasText(tradeDetailNo), TradeDetailDO::getTradeDetailNo, tradeDetailNo)
                .eq(StringUtils.hasText(receiptNo), TradeDetailDO::getReceiptNo, receiptNo)
                .eq(direction != null, TradeDetailDO::getDirection, direction == null ? null : direction.getCode())
                .ge(startDate != null, TradeDetailDO::getTradeTime, startDate == null ? null : startDate.atStartOfDay())
                .lt(endDate != null, TradeDetailDO::getTradeTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .orderByDesc(TradeDetailDO::getTradeTime)
                .last("LIMIT " + Math.max(maxRows, 0));
        return TradeDetailConverter.INSTANCE.toTradeDetailList(tradeDetailMapper.selectList(wrapper));
    }

    @Override
    public List<AggregateRow> aggregate(String tenantId, String accountNo, TradeDirection direction,
                                        LocalDate startDate, LocalDate endDate, AggregateSql aggregateSql, int maxRows) {
        List<AggregateRowDO> rows = tradeDetailMapper.aggregate(tenantId, StringUtils.hasText(accountNo) ? accountNo : null,
                direction == null ? null : direction.getCode(), startDate == null ? null : startDate.atStartOfDay(),
                endDate == null ? null : endDate.plusDays(1).atStartOfDay(), aggregateSql.getAggregateSql(), Math.max(maxRows, 0));
        return AggregateRowConverter.INSTANCE.toAggregateRowList(rows);
    }

}
