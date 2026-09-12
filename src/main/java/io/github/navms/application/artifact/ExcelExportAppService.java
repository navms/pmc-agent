package io.github.navms.application.artifact;

import cn.idev.excel.FastExcel;
import io.github.navms.application.artifact.dto.ArtifactInfo;
import io.github.navms.application.artifact.excel.AccountExcelRow;
import io.github.navms.application.artifact.excel.AggregateExcelRow;
import io.github.navms.application.artifact.excel.PayOrderExcelRow;
import io.github.navms.application.artifact.excel.TradeDetailExcelRow;
import io.github.navms.application.bank.dto.AccountQuery;
import io.github.navms.application.bank.dto.BankAggregateInfo;
import io.github.navms.application.bank.dto.BankAggregateQuery;
import io.github.navms.application.bank.dto.BankPayOrderQuery;
import io.github.navms.application.bank.dto.BankQueryResult;
import io.github.navms.application.bank.dto.TradeDetailQuery;
import io.github.navms.application.bank.service.BankAggregateAppService;
import io.github.navms.application.bank.support.BankQuerySupport;
import io.github.navms.application.bank.support.BankQuerySupport.DateRange;
import io.github.navms.domain.bank.BankDefaults;
import io.github.navms.domain.bank.entity.Account;
import io.github.navms.domain.bank.entity.BankPayOrder;
import io.github.navms.domain.bank.entity.TradeDetail;
import io.github.navms.domain.bank.enums.AccountStatus;
import io.github.navms.domain.bank.enums.PayOrderStatus;
import io.github.navms.domain.bank.enums.TradeDirection;
import io.github.navms.domain.bank.repository.AccountRepository;
import io.github.navms.domain.bank.repository.BankPayOrderRepository;
import io.github.navms.domain.bank.repository.TradeDetailRepository;
import io.github.navms.utils.enums.BaseEnum;
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

    private final ArtifactAppService artifactAppService;

    private final TradeDetailRepository tradeDetailRepository;

    private final BankPayOrderRepository bankPayOrderRepository;

    private final AccountRepository accountRepository;

    private final BankAggregateAppService bankAggregateAppService;

    /**
     * @param query 条件
     * @return 文件
     */
    public ArtifactInfo exportTradeDetails(TradeDetailQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return fail(dates.error());
        }
        List<TradeDetail> rows = tradeDetailRepository.query(
                BankQuerySupport.resolveTenant(query.tenantId()),
                BankQuerySupport.blankToNull(query.accountNo()),
                BankQuerySupport.blankToNull(query.tradeDetailNo()),
                BankQuerySupport.blankToNull(query.receiptNo()),
                BaseEnum.of(TradeDirection.class, query.direction()),
                dates.start(),
                dates.end(),
                BankDefaults.MAX_EXPORT_ROWS + 1);
        boolean truncated = rows.size() > BankDefaults.MAX_EXPORT_ROWS;
        if (truncated) {
            rows = rows.subList(0, BankDefaults.MAX_EXPORT_ROWS);
        }
        List<TradeDetailExcelRow> excelRows = new ArrayList<>(rows.size());
        for (TradeDetail row : rows) {
            TradeDetailExcelRow excelRow = new TradeDetailExcelRow();
            excelRow.setTradeDetailNo(row.getTradeDetailNo());
            excelRow.setAccountNo(row.getAccountNo());
            excelRow.setDirection(row.getDirection() == null ? null : row.getDirection().getCode());
            excelRow.setAmount(row.getAmount());
            excelRow.setCurrency(row.getCurrency());
            excelRow.setCounterpartName(row.getCounterpartName());
            excelRow.setSummary(row.getSummary());
            excelRow.setTradeTime(row.getTradeTime());
            excelRow.setBalanceAfter(row.getBalanceAfter());
            excelRows.add(excelRow);
        }
        return write("交易明细.xlsx", "交易明细", TradeDetailExcelRow.class, excelRows, truncated);
    }

    /**
     * @param query 条件
     * @return 文件
     */
    public ArtifactInfo exportPayOrders(BankPayOrderQuery query) {
        DateRange dates = BankQuerySupport.parseRange(query.startDate(), query.endDate());
        if (dates.error() != null) {
            return fail(dates.error());
        }
        List<BankPayOrder> rows = bankPayOrderRepository.query(
                BankQuerySupport.resolveTenant(query.tenantId()),
                BankQuerySupport.blankToNull(query.bankPayOrderNo()),
                BankQuerySupport.blankToNull(query.accountNo()),
                BaseEnum.of(PayOrderStatus.class, query.status()),
                dates.start(),
                dates.end(),
                BankDefaults.MAX_EXPORT_ROWS + 1);
        boolean truncated = rows.size() > BankDefaults.MAX_EXPORT_ROWS;
        if (truncated) {
            rows = rows.subList(0, BankDefaults.MAX_EXPORT_ROWS);
        }
        List<PayOrderExcelRow> excelRows = new ArrayList<>(rows.size());
        for (BankPayOrder row : rows) {
            PayOrderExcelRow excelRow = new PayOrderExcelRow();
            excelRow.setBankPayOrderNo(row.getBankPayOrderNo());
            excelRow.setAccountNo(row.getAccountNo());
            excelRow.setPayerName(row.getPayerName());
            excelRow.setTotalAmount(row.getTotalAmount());
            excelRow.setTotalCount(row.getTotalCount());
            excelRow.setStatus(row.getStatus() == null ? null : row.getStatus().getCode());
            excelRow.setPurpose(row.getPurpose());
            excelRow.setApplyTime(row.getApplyTime());
            excelRows.add(excelRow);
        }
        return write("银行支付单.xlsx", "支付单", PayOrderExcelRow.class, excelRows, truncated);
    }

    /**
     * @param query 条件
     * @return 文件
     */
    public ArtifactInfo exportAccounts(AccountQuery query) {
        List<Account> rows = accountRepository.query(
                BankQuerySupport.resolveTenant(query.tenantId()),
                BankQuerySupport.blankToNull(query.accountNo()),
                BankQuerySupport.blankToNull(query.accountName()),
                BaseEnum.of(AccountStatus.class, query.status()),
                BankDefaults.MAX_EXPORT_ROWS);
        List<AccountExcelRow> excelRows = new ArrayList<>(rows.size());
        for (Account row : rows) {
            AccountExcelRow excelRow = new AccountExcelRow();
            excelRow.setAccountNo(row.getAccountNo());
            excelRow.setAccountName(row.getAccountName());
            excelRow.setBankName(row.getBankName());
            excelRow.setAccountType(row.getAccountType() == null ? null : row.getAccountType().getCode());
            excelRow.setStatus(row.getStatus() == null ? null : row.getStatus().getCode());
            excelRow.setBalance(row.getBalance());
            excelRow.setAvailableBalance(row.getAvailableBalance());
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
        BankQueryResult<BankAggregateInfo> result = bankAggregateAppService.summarize(dataset, query);
        if (!result.success()) {
            return fail(result.message());
        }
        List<AggregateExcelRow> excelRows = new ArrayList<>();
        for (BankAggregateInfo row : result.data()) {
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
