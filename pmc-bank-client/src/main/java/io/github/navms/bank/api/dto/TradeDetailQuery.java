package io.github.navms.bank.api.dto;

/**
 * 交易明细查询。
 *
 * @param tenantId      租户 ID
 * @param accountNo     账号
 * @param tradeDetailNo 交易明细号
 * @param receiptNo     回单号
 * @param direction     借贷方向
 * @param startDate     交易日起 yyyy-MM-dd
 * @param endDate       交易日止 yyyy-MM-dd
 * @param maxRows       最大返回行数，空则使用默认查询上限
 * @author navms
 */
public record TradeDetailQuery(
        String tenantId,
        String accountNo,
        String tradeDetailNo,
        String receiptNo,
        String direction,
        String startDate,
        String endDate,
        Integer maxRows
) {
}
