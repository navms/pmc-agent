package io.github.navms.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.navms.domain.bank.entity.BalanceFlow;
import io.github.navms.domain.bank.repository.BalanceFlowRepository;
import io.github.navms.infrastructure.converter.BalanceFlowConverter;
import io.github.navms.infrastructure.mapper.BalanceFlowMapper;
import io.github.navms.infrastructure.model.BalanceFlowDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 余额流水仓储。
 *
 * @author navms
 */
@Repository
@RequiredArgsConstructor
public class BalanceFlowRepositoryImpl implements BalanceFlowRepository {

    private final BalanceFlowMapper balanceFlowMapper;

    @Override
    public List<BalanceFlow> query(String tenantId, String accountNo, LocalDate startDate, LocalDate endDate, int maxRows) {
        LambdaQueryWrapper<BalanceFlowDO> wrapper = new LambdaQueryWrapper<BalanceFlowDO>()
                .eq(BalanceFlowDO::getTenantId, tenantId)
                .eq(StringUtils.hasText(accountNo), BalanceFlowDO::getAccountNo, accountNo)
                .ge(startDate != null, BalanceFlowDO::getOccurTime, startDate == null ? null : startDate.atStartOfDay())
                .lt(endDate != null, BalanceFlowDO::getOccurTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .orderByDesc(BalanceFlowDO::getOccurTime)
                .last("LIMIT " + Math.max(maxRows, 0));
        return BalanceFlowConverter.INSTANCE.toBalanceFlowList(balanceFlowMapper.selectList(wrapper));
    }
}
