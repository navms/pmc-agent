package io.github.navms.bank.api;

/**
 * 银企查询条数上限，供调用方（如导出）显式传入 {@code maxRows}。
 *
 * @author navms
 */
public final class BankLimits {

    public static final int MAX_QUERY_ROWS = 10;

    public static final int MAX_EXPORT_ROWS = 20_000;

    public static final int MAX_AGGREGATE_ROWS = 500;

}
