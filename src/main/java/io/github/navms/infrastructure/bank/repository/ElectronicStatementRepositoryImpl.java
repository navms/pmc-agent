package io.github.navms.infrastructure.bank.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.navms.domain.bank.BankDefaults;
import io.github.navms.domain.bank.entity.ElectronicStatement;
import io.github.navms.domain.bank.enums.AggregateGroup;
import io.github.navms.domain.bank.enums.StatementStatus;
import io.github.navms.domain.bank.repository.ElectronicStatementRepository;
import io.github.navms.domain.bank.valueobj.BankAggregateRow;
import io.github.navms.infrastructure.bank.converter.ElectronicStatementConverter;
import io.github.navms.infrastructure.bank.mapper.ElectronicStatementMapper;
import io.github.navms.infrastructure.bank.model.ElectronicStatementDO;
import io.github.navms.infrastructure.bank.support.AggregateRows;
import io.github.navms.infrastructure.bank.support.AggregateSql;
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
    public List<ElectronicStatement> query(
            String tenantId,
            String statementNo,
            String tradeDetailNo,
            String accountNo,
            LocalDate periodStart,
            LocalDate periodEnd) {
        LambdaQueryWrapper<ElectronicStatementDO> wrapper = new LambdaQueryWrapper<ElectronicStatementDO>()
                .eq(ElectronicStatementDO::getTenantId, tenantId)
                .eq(StringUtils.hasText(statementNo), ElectronicStatementDO::getStatementNo, statementNo)
                .eq(StringUtils.hasText(tradeDetailNo), ElectronicStatementDO::getTradeDetailNo, tradeDetailNo)
                .eq(StringUtils.hasText(accountNo), ElectronicStatementDO::getAccountNo, accountNo)
                .ge(periodStart != null, ElectronicStatementDO::getPeriodEnd, periodStart)
                .le(periodEnd != null, ElectronicStatementDO::getPeriodStart, periodEnd)
                .orderByDesc(ElectronicStatementDO::getPeriodStart)
                .last("LIMIT " + BankDefaults.MAX_QUERY_ROWS);
        return ElectronicStatementConverter.INSTANCE.toElectronicStatementList(
                electronicStatementMapper.selectList(wrapper));
    }

    @Override
    public List<BankAggregateRow> aggregate(
            String tenantId,
            String accountNo,
            StatementStatus status,
            LocalDate startDate,
            LocalDate endDate,
            AggregateGroup group,
            int maxRows) {
        return AggregateRows.toDomain(electronicStatementMapper.aggregate(
                tenantId,
                StringUtils.hasText(accountNo) ? accountNo : null,
                status == null ? null : status.getCode(),
                startDate == null ? null : startDate.atStartOfDay(),
                endDate == null ? null : endDate.plusDays(1).atStartOfDay(),
                AggregateSql.statement(group),
                Math.max(maxRows, 0)));
    }
}
