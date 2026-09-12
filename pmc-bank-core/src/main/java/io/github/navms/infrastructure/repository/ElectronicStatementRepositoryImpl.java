package io.github.navms.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.navms.domain.bank.entity.ElectronicStatement;
import io.github.navms.domain.bank.enums.StatementStatus;
import io.github.navms.domain.bank.repository.ElectronicStatementRepository;
import io.github.navms.domain.bank.valueobj.AggregateRow;
import io.github.navms.infrastructure.converter.AggregateRowConverter;
import io.github.navms.infrastructure.converter.ElectronicStatementConverter;
import io.github.navms.infrastructure.mapper.ElectronicStatementMapper;
import io.github.navms.infrastructure.model.AggregateRowDO;
import io.github.navms.infrastructure.model.ElectronicStatementDO;
import io.github.navms.infrastructure.support.AggregateSql;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 电子对账单仓储。
 *
 * @author navms
 */
@Repository
@RequiredArgsConstructor
public class ElectronicStatementRepositoryImpl implements ElectronicStatementRepository {

    private final ElectronicStatementMapper electronicStatementMapper;

    @Override
    public List<ElectronicStatement> query(String tenantId, String statementNo, String tradeDetailNo,
                                           String accountNo, LocalDate periodStart, LocalDate periodEnd, int maxRows) {
        LambdaQueryWrapper<ElectronicStatementDO> wrapper = new LambdaQueryWrapper<ElectronicStatementDO>()
                .eq(ElectronicStatementDO::getTenantId, tenantId)
                .eq(StringUtils.hasText(statementNo), ElectronicStatementDO::getStatementNo, statementNo)
                .eq(StringUtils.hasText(tradeDetailNo), ElectronicStatementDO::getTradeDetailNo, tradeDetailNo)
                .eq(StringUtils.hasText(accountNo), ElectronicStatementDO::getAccountNo, accountNo)
                .ge(periodStart != null, ElectronicStatementDO::getPeriodEnd, periodStart)
                .le(periodEnd != null, ElectronicStatementDO::getPeriodStart, periodEnd)
                .orderByDesc(ElectronicStatementDO::getPeriodStart)
                .last("LIMIT " + Math.max(maxRows, 0));
        return ElectronicStatementConverter.INSTANCE.toElectronicStatementList(electronicStatementMapper.selectList(wrapper));
    }

    @Override
    public List<AggregateRow> aggregate(String tenantId, String accountNo, StatementStatus status,
                                        LocalDate startDate, LocalDate endDate, AggregateSql aggregateSql, int maxRows) {
        List<AggregateRowDO> rows = electronicStatementMapper.aggregate(
                tenantId, StringUtils.hasText(accountNo) ? accountNo : null,
                status == null ? null : status.getCode(), startDate == null ? null : startDate.atStartOfDay(),
                endDate == null ? null : endDate.plusDays(1).atStartOfDay(), aggregateSql.getAggregateSql(), Math.max(maxRows, 0));
        return AggregateRowConverter.INSTANCE.toAggregateRowList(rows);
    }
}
