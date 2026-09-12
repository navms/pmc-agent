package io.github.navms.infrastructure.bank.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.navms.domain.bank.entity.Account;
import io.github.navms.domain.bank.enums.AccountStatus;
import io.github.navms.domain.bank.enums.AggregateGroup;
import io.github.navms.domain.bank.repository.AccountRepository;
import io.github.navms.domain.bank.valueobj.BankAggregateRow;
import io.github.navms.infrastructure.bank.converter.AccountConverter;
import io.github.navms.infrastructure.bank.mapper.AccountMapper;
import io.github.navms.infrastructure.bank.model.AccountDO;
import io.github.navms.infrastructure.bank.support.AggregateRows;
import io.github.navms.infrastructure.bank.support.AggregateSql;
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
    public List<BankAggregateRow> aggregate(
            String tenantId,
            String accountNo,
            AccountStatus status,
            AggregateGroup group,
            int maxRows) {
        return AggregateRows.toDomain(accountMapper.aggregate(
                tenantId,
                StringUtils.hasText(accountNo) ? accountNo : null,
                status == null ? null : status.getCode(),
                AggregateSql.account(group),
                Math.max(maxRows, 0)));
    }
}
