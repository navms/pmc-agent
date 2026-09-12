package io.github.navms.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.navms.domain.bank.entity.Account;
import io.github.navms.domain.bank.enums.AccountStatus;
import io.github.navms.domain.bank.repository.AccountRepository;
import io.github.navms.domain.bank.valueobj.AggregateRow;
import io.github.navms.infrastructure.converter.AccountConverter;
import io.github.navms.infrastructure.converter.AggregateRowConverter;
import io.github.navms.infrastructure.mapper.AccountMapper;
import io.github.navms.infrastructure.model.AccountDO;
import io.github.navms.infrastructure.model.AggregateRowDO;
import io.github.navms.infrastructure.support.AggregateSql;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 账户仓储。
 *
 * @author navms
 */
@Repository
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {

    private final AccountMapper accountMapper;

    @Override
    public List<Account> query(String tenantId, String accountNo, String accountName, AccountStatus status, int maxRows) {
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .eq(StringUtils.hasText(accountNo), AccountDO::getAccountNo, accountNo)
                .like(StringUtils.hasText(accountName), AccountDO::getAccountName, accountName)
                .eq(status != null, AccountDO::getStatus, status == null ? null : status.getCode())
                .orderByAsc(AccountDO::getAccountNo)
                .last("LIMIT " + Math.max(maxRows, 0));
        return AccountConverter.INSTANCE.toAccountList(accountMapper.selectList(wrapper));
    }

    @Override
    public List<AggregateRow> aggregate(String tenantId, String accountNo, AccountStatus status, AggregateSql aggregateSql, int maxRows) {
        List<AggregateRowDO> rows = accountMapper.aggregate(tenantId,
                StringUtils.hasText(accountNo) ? accountNo : null, status == null ? null : status.getCode(),
                aggregateSql.getAggregateSql(), Math.max(maxRows, 0));
        return AggregateRowConverter.INSTANCE.toAggregateRowList(rows);
    }

}
