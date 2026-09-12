package io.github.navms.domain.bank;

/**
 * 银企查询默认值。
 *
 * @author navms
 */
public final class BankDefaults {

    public static final String DEFAULT_TENANT_ID = "T001";

    public static final int MAX_QUERY_ROWS = 10;

    public static final int MAX_EXPORT_ROWS = 20_000;

    public static final int MAX_AGGREGATE_ROWS = 500;

    private BankDefaults() {
    }
}
