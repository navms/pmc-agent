package io.github.navms.application.artifact;

import cn.idev.excel.FastExcel;
import io.github.navms.application.artifact.dto.ArtifactInfo;
import io.github.navms.application.artifact.excel.AccountExcelRow;
import io.github.navms.application.artifact.excel.AggregateExcelRow;
import io.github.navms.application.artifact.excel.PayOrderExcelRow;
import io.github.navms.application.artifact.excel.TradeDetailExcelRow;
import io.github.navms.bank.api.BankAggregateApi;
import io.github.navms.bank.api.BankLimits;
import io.github.navms.bank.api.BankQueryApi;
import io.github.navms.bank.api.dto.AccountInfo;
import io.github.navms.bank.api.dto.AccountQuery;
import io.github.navms.bank.api.dto.AggregateInfo;
import io.github.navms.bank.api.dto.BankAggregateQuery;
import io.github.navms.bank.api.dto.BankPayOrderInfo;
import io.github.navms.bank.api.dto.BankPayOrderQuery;
import io.github.navms.bank.api.dto.BankQueryResult;
import io.github.navms.bank.api.dto.TradeDetailInfo;
import io.github.navms.bank.api.dto.TradeDetailQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * FastExcel 导出。
 *
 * @author navms
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelExportAppService {

    private static final int EXPORT_FETCH = BankLimits.MAX_EXPORT_ROWS + 1;

    private final ArtifactAppService artifactAppService;

    private final BankQueryApi bankQueryApi;

    private final BankAggregateApi bankAggregateApi;

    /**
     * @param query 条件
     * @return 文件
     */
    public ArtifactInfo exportTradeDetails(TradeDetailQuery query) {
        BankQueryResult<TradeDetailInfo> result = bankQueryApi.queryTradeDetails(
                new TradeDetailQuery(
                        query.tenantId(),
                        query.accountNo(),
                        query.tradeDetailNo(),
                        query.receiptNo(),
                        query.direction(),
                        query.startDate(),
                        query.endDate(),
                        EXPORT_FETCH));
        if (!result.success()) {
            return fail(result.message());
        }
        List<TradeDetailInfo> rows = result.data();
        boolean truncated = rows.size() > BankLimits.MAX_EXPORT_ROWS;
        if (truncated) {
            rows = rows.subList(0, BankLimits.MAX_EXPORT_ROWS);
        }
        List<TradeDetailExcelRow> excelRows = new ArrayList<>(rows.size());
        for (TradeDetailInfo row : rows) {
            TradeDetailExcelRow excelRow = new TradeDetailExcelRow();
            excelRow.setTradeDetailNo(row.tradeDetailNo());
            excelRow.setAccountNo(row.accountNo());
            excelRow.setDirection(row.direction());
            excelRow.setAmount(row.amount());
            excelRow.setCurrency(row.currency());
            excelRow.setCounterpartName(row.counterpartName());
            excelRow.setSummary(row.summary());
            excelRow.setTradeTime(row.tradeTime());
            excelRow.setBalanceAfter(row.balanceAfter());
            excelRows.add(excelRow);
        }
        return write("交易明细.xlsx", "交易明细", TradeDetailExcelRow.class, excelRows, truncated);
    }

    /**
     * @param query 条件
     * @return 文件
     */
    public ArtifactInfo exportPayOrders(BankPayOrderQuery query) {
        BankQueryResult<BankPayOrderInfo> result = bankQueryApi.queryBankPayOrders(
                new BankPayOrderQuery(
                        query.tenantId(),
                        query.bankPayOrderNo(),
                        query.accountNo(),
                        query.status(),
                        query.startDate(),
                        query.endDate(),
                        EXPORT_FETCH));
        if (!result.success()) {
            return fail(result.message());
        }
        List<BankPayOrderInfo> rows = result.data();
        boolean truncated = rows.size() > BankLimits.MAX_EXPORT_ROWS;
        if (truncated) {
            rows = rows.subList(0, BankLimits.MAX_EXPORT_ROWS);
        }
        List<PayOrderExcelRow> excelRows = new ArrayList<>(rows.size());
        for (BankPayOrderInfo row : rows) {
            PayOrderExcelRow excelRow = new PayOrderExcelRow();
            excelRow.setBankPayOrderNo(row.bankPayOrderNo());
            excelRow.setAccountNo(row.accountNo());
            excelRow.setPayerName(row.payerName());
            excelRow.setTotalAmount(row.totalAmount());
            excelRow.setTotalCount(row.totalCount());
            excelRow.setStatus(row.status());
            excelRow.setPurpose(row.purpose());
            excelRow.setApplyTime(row.applyTime());
            excelRows.add(excelRow);
        }
        return write("银行支付单.xlsx", "支付单", PayOrderExcelRow.class, excelRows, truncated);
    }

    /**
     * @param query 条件
     * @return 文件
     */
    public ArtifactInfo exportAccounts(AccountQuery query) {
        BankQueryResult<AccountInfo> result = bankQueryApi.queryAccounts(
                new AccountQuery(
                        query.tenantId(),
                        query.accountNo(),
                        query.accountName(),
                        query.status(),
                        BankLimits.MAX_EXPORT_ROWS));
        if (!result.success()) {
            return fail(result.message());
        }
        List<AccountExcelRow> excelRows = new ArrayList<>();
        for (AccountInfo row : result.data()) {
            AccountExcelRow excelRow = new AccountExcelRow();
            excelRow.setAccountNo(row.accountNo());
            excelRow.setAccountName(row.accountName());
            excelRow.setBankName(row.bankName());
            excelRow.setAccountType(row.accountType());
            excelRow.setStatus(row.status());
            excelRow.setBalance(row.balance());
            excelRow.setAvailableBalance(row.availableBalance());
            excelRows.add(excelRow);
        }
        return write("银行账户.xlsx", "账户", AccountExcelRow.class, excelRows, false);
    }

    /**
     * @param dataset trade / pay_order
     * @param query   汇总条件
     * @return 文件
     */
    public ArtifactInfo exportSummary(String dataset, BankAggregateQuery query) {
        BankQueryResult<AggregateInfo> result = bankAggregateApi.summarize(dataset, query);
        if (!result.success()) {
            return fail(result.message());
        }
        List<AggregateExcelRow> excelRows = new ArrayList<>();
        for (AggregateInfo row : result.data()) {
            AggregateExcelRow excelRow = new AggregateExcelRow();
            excelRow.setBucket(row.bucket());
            excelRow.setRowCount(row.rowCount());
            excelRow.setTotalAmount(row.totalAmount());
            excelRows.add(excelRow);
        }
        return write("银企汇总.xlsx", "汇总", AggregateExcelRow.class, excelRows, false);
    }

    private ArtifactInfo write(String fileName, String sheetName, Class<?> head, List<?> rows, boolean truncated) {
        try {
            ArtifactAppService.PreparedFile prepared = artifactAppService.prepare(fileName);
            FastExcel.write(prepared.path().toFile(), head)
                    .sheet(sheetName)
                    .doWrite(rows);
            return artifactAppService.toInfo(prepared.fileId(), fileName, rows.size(), truncated);
        } catch (Exception ex) {
            log.error("Excel export failed", ex);
            return fail("Excel 导出失败：" + ex.getMessage());
        }
    }

    private static ArtifactInfo fail(String message) {
        return new ArtifactInfo(false, message, null, null, null, 0, false);
    }
}
