package io.github.navms.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.navms.domain.bank.entity.ElectronicReceipt;
import io.github.navms.domain.bank.repository.ElectronicReceiptRepository;
import io.github.navms.domain.bank.valueobj.AggregateRow;
import io.github.navms.infrastructure.converter.AggregateRowConverter;
import io.github.navms.infrastructure.converter.ElectronicReceiptConverter;
import io.github.navms.infrastructure.mapper.ElectronicReceiptMapper;
import io.github.navms.infrastructure.model.AggregateRowDO;
import io.github.navms.infrastructure.model.ElectronicReceiptDO;
import io.github.navms.infrastructure.support.AggregateSql;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 电子回单仓储。
 *
 * @author navms
 */
@Repository
@RequiredArgsConstructor
public class ElectronicReceiptRepositoryImpl implements ElectronicReceiptRepository {

    private final ElectronicReceiptMapper electronicReceiptMapper;

    @Override
    public List<ElectronicReceipt> query(String tenantId, String receiptNo, String tradeDetailNo,
                                         String accountNo, LocalDate startDate, LocalDate endDate, int maxRows) {
        LambdaQueryWrapper<ElectronicReceiptDO> wrapper = new LambdaQueryWrapper<ElectronicReceiptDO>()
                .eq(ElectronicReceiptDO::getTenantId, tenantId)
                .eq(StringUtils.hasText(receiptNo), ElectronicReceiptDO::getReceiptNo, receiptNo)
                .eq(StringUtils.hasText(tradeDetailNo), ElectronicReceiptDO::getTradeDetailNo, tradeDetailNo)
                .eq(StringUtils.hasText(accountNo), ElectronicReceiptDO::getAccountNo, accountNo)
                .ge(startDate != null, ElectronicReceiptDO::getIssueTime, startDate == null ? null : startDate.atStartOfDay())
                .lt(endDate != null, ElectronicReceiptDO::getIssueTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .orderByDesc(ElectronicReceiptDO::getIssueTime)
                .last("LIMIT " + Math.max(maxRows, 0));
        return ElectronicReceiptConverter.INSTANCE.toElectronicReceiptList(electronicReceiptMapper.selectList(wrapper));
    }

    @Override
    public List<AggregateRow> aggregate(String tenantId, String accountNo,
                                        LocalDate startDate, LocalDate endDate, AggregateSql aggregateSql, int maxRows) {
        List<AggregateRowDO> rows = electronicReceiptMapper.aggregate(tenantId,
                StringUtils.hasText(accountNo) ? accountNo : null, startDate == null ? null : startDate.atStartOfDay(),
                endDate == null ? null : endDate.plusDays(1).atStartOfDay(), aggregateSql.getAggregateSql(), Math.max(maxRows, 0));
        return AggregateRowConverter.INSTANCE.toAggregateRowList(rows);
    }
}
