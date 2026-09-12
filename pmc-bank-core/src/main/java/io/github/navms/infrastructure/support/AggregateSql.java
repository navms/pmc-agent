package io.github.navms.infrastructure.support;

import io.github.navms.domain.bank.enums.AggregateGroup;

/**
 * 将分组枚举映射为白名单 SQL 表达式。
 *
 * @author navms
 */
@FunctionalInterface
public interface AggregateSql {

    String getAggregateSql();

    /**
     * @param groupBy 分组
     * @return 交易明细 GROUP BY 表达式
     */
    static String tradeDetail(String groupBy) {
        AggregateGroup group = AggregateGroup.resolveGroup(groupBy, AggregateGroup.ACCOUNT);
        return switch (group) {
            case ACCOUNT -> "account_no";
            case DAY -> "DATE(trade_time)";
            case MONTH -> "DATE_FORMAT(trade_time, '%Y-%m')";
            case YEAR -> "DATE_FORMAT(trade_time, '%Y')";
            case DIRECTION -> "direction";
            case COUNTERPART -> "IFNULL(counterpart_name, '未知')";
            case STATUS, TYPE, BANK -> throw new IllegalArgumentException(
                    "交易明细不支持该分组，请使用 account/day/month/direction/counterpart");
        };
    }

    /**
     * @param groupBy 分组
     * @return 支付单 GROUP BY 表达式
     */
    static String payOrder(String groupBy) {
        AggregateGroup group = AggregateGroup.resolveGroup(groupBy, AggregateGroup.STATUS);
        return switch (group) {
            case ACCOUNT -> "account_no";
            case DAY -> "DATE(apply_time)";
            case MONTH -> "DATE_FORMAT(apply_time, '%Y-%m')";
            case YEAR -> "DATE_FORMAT(apply_time, '%Y')";
            case STATUS -> "status";
            case DIRECTION, COUNTERPART, TYPE, BANK -> throw new IllegalArgumentException(
                    "支付单不支持该分组，请使用 account/day/month/status");
        };
    }

    /**
     * @param groupBy 分组
     * @return 账户 GROUP BY 表达式
     */
    static String account(String groupBy) {
        AggregateGroup group = AggregateGroup.resolveGroup(groupBy, AggregateGroup.STATUS);
        return switch (group) {
            case ACCOUNT -> "account_no";
            case STATUS -> "status";
            case TYPE -> "account_type";
            case BANK -> "IFNULL(bank_name, '未知')";
            case DAY, MONTH, YEAR, DIRECTION, COUNTERPART -> throw new IllegalArgumentException(
                    "账户不支持该分组，请使用 status/type/bank/account");
        };
    }

    /**
     * @param groupBy 分组
     * @return 电子回单 GROUP BY 表达式
     */
    static String receipt(String groupBy) {
        AggregateGroup group = AggregateGroup.resolveGroup(groupBy, AggregateGroup.ACCOUNT);
        return switch (group) {
            case ACCOUNT -> "account_no";
            case DAY -> "DATE(issue_time)";
            case MONTH -> "DATE_FORMAT(issue_time, '%Y-%m')";
            case YEAR -> "DATE_FORMAT(issue_time, '%Y')";
            case DIRECTION, COUNTERPART, STATUS, TYPE, BANK -> throw new IllegalArgumentException(
                    "电子回单不支持该分组，请使用 account/day/month");
        };
    }

    /**
     * @param groupBy 分组
     * @return 电子对账单 GROUP BY 表达式
     */
    static String statement(String groupBy) {
        AggregateGroup group = AggregateGroup.resolveGroup(groupBy, AggregateGroup.ACCOUNT);
        return switch (group) {
            case ACCOUNT -> "account_no";
            case STATUS -> "status";
            case DAY -> "DATE(issue_time)";
            case MONTH -> "DATE_FORMAT(issue_time, '%Y-%m')";
            case YEAR -> "DATE_FORMAT(issue_time, '%Y')";
            case DIRECTION, COUNTERPART, TYPE, BANK -> throw new IllegalArgumentException(
                    "电子对账单不支持该分组，请使用 account/status/day/month");
        };
    }

    /**
     * @param groupBy 分组
     * @return 支付明细 GROUP BY 表达式
     */
    static String payOrderDetail(String groupBy) {
        AggregateGroup group = AggregateGroup.resolveGroup(groupBy, AggregateGroup.STATUS);
        return switch (group) {
            case ACCOUNT -> "account_no";
            case STATUS -> "status";
            case COUNTERPART -> "IFNULL(payee_name, '未知')";
            case DAY -> "DATE(created_at)";
            case MONTH -> "DATE_FORMAT(created_at, '%Y-%m')";
            case YEAR -> "DATE_FORMAT(created_at, '%Y')";
            case DIRECTION, TYPE, BANK -> throw new IllegalArgumentException(
                    "支付明细不支持该分组，请使用 account/status/counterpart/day/month");
        };
    }

}
