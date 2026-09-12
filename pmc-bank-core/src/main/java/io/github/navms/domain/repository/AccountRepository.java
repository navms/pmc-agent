package io.github.navms.domain.bank.repository;

import io.github.navms.domain.bank.entity.Account;
import io.github.navms.domain.bank.enums.AccountStatus;
import io.github.navms.domain.bank.valueobj.AggregateRow;
import io.github.navms.infrastructure.support.AggregateSql;

import java.util.List;

/**
 * 账户查询仓储。
 *
 * @author navms
 */
public interface AccountRepository {

    /**
     * @param tenantId    租户
     * @param accountNo   账号，可空
     * @param accountName 户名模糊，可空
     * @param status      状态，可空
     * @param maxRows     最大行数
     * @return 账户列表
     */
    List<Account> query(String tenantId, String accountNo, String accountName, AccountStatus status, int maxRows);

    /**
     * @param tenantId     租户
     * @param accountNo    账号，可空
     * @param status       状态，可空
     * @param aggregateSql 聚合 SQL
     * @param maxRows      最大分组数
     * @return 聚合结果，金额为余额合计
     */
    List<AggregateRow> aggregate(String tenantId, String accountNo, AccountStatus status, AggregateSql aggregateSql, int maxRows);
}
