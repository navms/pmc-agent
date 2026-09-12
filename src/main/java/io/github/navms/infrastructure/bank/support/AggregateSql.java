package io.github.navms.infrastructure.bank.support;

import io.github.navms.domain.bank.enums.AggregateGroup;

/**
 * 将分组枚举映射为白名单 SQL 表达式。
 *
 * @author navms
 */
public final class AggregateSql {

    /**
     * @param group 分组
     * @return 交易明细 GROUP BY 表达式
     */
    public static String tradeDetail(AggregateGroup group) {
        return switch (group) {
            case ACCOUNT -> "account_no";
            case DAY -> "DATE(trade_time)";
            case MONTH -> "DATE_FORMAT(trade_time, '%Y-%m')";
            case DIRECTION -> "direction";
            case COUNTERPART -> "IFNULL(counterpart_name, '未知')";
            case STATUS, TYPE, BANK -> throw new IllegalArgumentException(
                    "交易明细不支持该分组，请使用 account/day/month/direction/counterpart");
        };
    }

    /**
     * @param group 分组
     * @return 支付单 GROUP BY 表达式
     */
    public static String payOrder(AggregateGroup group) {
        return switch (group) {
            case ACCOUNT -> "account_no";
            case DAY -> "DATE(apply_time)";
            case MONTH -> "DATE_FORMAT(apply_time, '%Y-%m')";
            case STATUS -> "status";
            case DIRECTION, COUNTERPART, TYPE, BANK -> throw new IllegalArgumentException(
                    "支付单不支持该分组，请使用 account/day/month/status");
        };
    }

    /**
     * @param group 分组
     * @return 账户 GROUP BY 表达式
     */
    public static String account(AggregateGroup group) {
        return switch (group) {
            case ACCOUNT -> "account_no";
            case STATUS -> "status";
            case TYPE -> "account_type";
            case BANK -> "IFNULL(bank_name, '未知')";
            case DAY, MONTH, DIRECTION, COUNTERPART -> throw new IllegalArgumentException(
                    "账户不支持该分组，请使用 status/type/bank/account");
        };
    }

    /**
     * @param group 分组
     * @return 电子回单 GROUP BY 表达式
     */
    public static String receipt(AggregateGroup group) {
        return switch (group) {
            case ACCOUNT -> "account_no";
            case DAY -> "DATE(issue_time)";
            case MONTH -> "DATE_FORMAT(issue_time, '%Y-%m')";
            case DIRECTION, COUNTERPART, STATUS, TYPE, BANK -> throw new IllegalArgumentException(
                    "电子回单不支持该分组，请使用 account/day/month");
        };
    }

    /**
     * @param group 分组
     * @return 电子对账单 GROUP BY 表达式
     */
    public static String statement(AggregateGroup group) {
        return switch (group) {
            case ACCOUNT -> "account_no";
            case STATUS -> "status";
            case DAY -> "DATE(issue_time)";
            case MONTH -> "DATE_FORMAT(issue_time, '%Y-%m')";
            case DIRECTION, COUNTERPART, TYPE, BANK -> throw new IllegalArgumentException(
                    "电子对账单不支持该分组，请使用 account/status/day/month");
        };
    }

    /**
     * @param group 分组
     * @return 支付明细 GROUP BY 表达式
     */
    public static String payOrderDetail(AggregateGroup group) {
        return switch (group) {
            case ACCOUNT -> "account_no";
            case STATUS -> "status";
            case COUNTERPART -> "IFNULL(payee_name, '未知')";
            case DAY -> "DATE(created_at)";
            case MONTH -> "DATE_FORMAT(created_at, '%Y-%m')";
            case DIRECTION, TYPE, BANK -> throw new IllegalArgumentException(
                    "支付明细不支持该分组，请使用 account/status/counterpart/day/month");
        };
    }
}
