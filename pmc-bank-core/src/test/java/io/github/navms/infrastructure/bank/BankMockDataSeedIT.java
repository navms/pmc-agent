package io.github.navms.infrastructure.bank;

import io.github.navms.BankCoreTestApplication;
import io.github.navms.domain.bank.Defaults;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 向真实库灌入医院银企场景数据。默认 mvn test 不执行，需指定 groups=seed。
 *
 * @author navms
 */
@Slf4j
@Tag("seed")
@SpringBootTest(classes = BankCoreTestApplication.class)
class BankMockDataSeedIT {

    private static final String TENANT = Defaults.DEFAULT_TENANT_ID;

    private static final int ACCOUNT_COUNT = 10;

    private static final int ORDER_COUNT = 100_000;

    private static final int DETAILS_PER_ORDER = 10;

    private static final int BATCH_SIZE = 1_000;

    private static final int CREDIT_EXTRA = 200;

    private static final int WORK_START_HOUR = 9;

    private static final int WORK_END_HOUR = 17;

    private static final String[] CREATORS = {"U1001", "U1002"};

    private static final String[][] ACCOUNTS = {
            {"6222021000000001001", "市第一人民医院基本户", "102", "中国工商银行", "工行某某支行", "basic", "18560000.00"},
            {"6227001000000002002", "市第一人民医院医保专户", "105", "中国建设银行", "建行某某支行", "special", "4280000.00"},
            {"6013821000000003003", "市第一人民医院科研专户", "104", "中国银行", "中行某某支行", "special", "2160000.00"},
            {"6222021000000004004", "市第一人民医院零余额账户", "102", "中国工商银行", "工行某某支行", "zero_balance", "0.00"},
            {"6225881000000005005", "市第一人民医院工会户", "308", "招商银行", "招行某某支行", "special", "860000.00"},
            {"6228481000000006006", "市第一人民医院食堂专户", "103", "中国农业银行", "农行某某支行", "special", "420000.00"},
            {"6222621000000007007", "市第一人民医院基建专户", "301", "交通银行", "交行某某支行", "special", "9600000.00"},
            {"6013821000000008008", "市第一人民医院捐赠专户", "104", "中国银行", "中行某某支行", "special", "310000.00"},
            {"6227001000000009009", "市第一人民医院培训专户", "105", "中国建设银行", "建行某某支行", "special", "180000.00"},
            {"6222021000000010010", "市第一人民医院药品备付户", "102", "中国工商银行", "工行某某支行", "special", "3520000.00"}
    };

    private static final String[][] PAYEES = {
            {"6225880000001111", "华康医疗器械有限公司", "招商银行某某支行", "医疗器械采购"},
            {"6228480000002222", "仁济药品股份有限公司", "农业银行某某支行", "药品采购"},
            {"6222020000003333", "安泰后勤服务有限公司", "工商银行某某支行", "物业服务费"},
            {"6013820000004444", "市供电公司", "中国银行某某支行", "电费"},
            {"6227000000005555", "市自来水公司", "建设银行某某支行", "水费"},
            {"6222620000006666", "博文劳务派遣有限公司", "交通银行某某支行", "劳务费"},
            {"6225880000007777", "绿源医疗废物处置有限公司", "招商银行某某支行", "医废处置"},
            {"6228480000008888", "康达试剂有限公司", "农业银行某某支行", "检验试剂"},
            {"6222020000009999", "星海软件科技有限公司", "工商银行某某支行", "信息系统服务"},
            {"6013820000001010", "正大建筑工程有限公司", "中国银行某某支行", "维修改造"}
    };

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 清空 T001 后写入 10 账户 / 10 万支付单 / 100 万明细及关联单据。
     */
    @Test
    void seedHospitalBankData() {
        wipeTenant();
        BigDecimal[] running = insertAccounts();
        Random rng = new Random(20250303L);
        LocalDateTime cursor = LocalDateTime.of(2025, 3, 3, 9, 0, 0);
        LocalDateTime lastEventTime = cursor;
        long tradeId = 1L;
        int successDetails = 0;
        int failedDetails = 0;

        List<Object[]> orderBatch = new ArrayList<>(BATCH_SIZE);
        List<Object[]> detailBatch = new ArrayList<>(BATCH_SIZE * DETAILS_PER_ORDER);
        List<Object[]> tradeBatch = new ArrayList<>(BATCH_SIZE * DETAILS_PER_ORDER);
        List<Object[]> receiptBatch = new ArrayList<>(BATCH_SIZE * DETAILS_PER_ORDER);
        List<Object[]> statementBatch = new ArrayList<>(BATCH_SIZE * DETAILS_PER_ORDER);
        List<Object[]> flowBatch = new ArrayList<>(BATCH_SIZE * DETAILS_PER_ORDER);

        for (int orderIndex = 1; orderIndex <= ORDER_COUNT; orderIndex++) {
            int accountIdx = (orderIndex - 1) % ACCOUNT_COUNT;
            String[] account = ACCOUNTS[accountIdx];
            String accountNo = account[0];
            String payerName = account[1];
            String bankName = account[3];
            String createdBy = CREATORS[orderIndex % 2];
            LocalDateTime applyTime = nextBusinessApplyTime(cursor, rng);
            cursor = applyTime;
            String orderNo = "BP" + pad(orderIndex, 10);

            BigDecimal total = BigDecimal.ZERO;
            PayOrderStatusAgg agg = PayOrderStatusAgg.SUCCESS;
            int failedInOrder = 0;
            LocalDateTime orderPayTime = randomPayTime(applyTime, rng);

            for (int seq = 1; seq <= DETAILS_PER_ORDER; seq++) {
                String[] payee = PAYEES[(orderIndex + seq) % PAYEES.length];
                BigDecimal amount = BigDecimal.valueOf(800 + ((orderIndex * 13L + seq * 17L) % 50_000))
                        .setScale(2, RoundingMode.HALF_UP);
                total = total.add(amount);
                boolean failed = (orderIndex * 10 + seq) % 13 == 0;
                String status = failed ? "failed" : "success";
                if (failed) {
                    failedInOrder++;
                    failedDetails++;
                    agg = failedInOrder == DETAILS_PER_ORDER ? PayOrderStatusAgg.FAILED : PayOrderStatusAgg.PARTIAL;
                } else {
                    successDetails++;
                }
                String detailNo = "TD" + pad(orderIndex, 8) + pad(seq, 2);
                long detailId = ((long) orderIndex - 1) * DETAILS_PER_ORDER + seq;
                String failReason = failed ? "收款账号校验失败" : null;
                LocalDateTime detailCreated = applyTime.plusSeconds(rng.nextInt(16));
                detailBatch.add(new Object[]{
                        detailId, TENANT, createdBy, detailCreated, detailCreated, 0,
                        (long) orderIndex, orderNo, detailNo, accountNo,
                        payee[0], payee[1], payee[2], amount, "CNY", status,
                        payee[3], seq, failReason
                });
                if (failed) {
                    continue;
                }
                String receiptNo = "RC" + pad(orderIndex, 8) + pad(seq, 2);
                LocalDateTime payTime = orderPayTime.plusSeconds(rng.nextInt(16));
                running[accountIdx] = running[accountIdx].subtract(amount);
                BigDecimal after = running[accountIdx];
                BigDecimal before = after.add(amount);
                tradeBatch.add(tradeRow(tradeId, createdBy, payTime, detailNo, receiptNo, accountNo,
                        "debit", amount, payee[0], payee[1], payee[2], payee[3], after, "BS" + pad(tradeId, 12)));
                receiptBatch.add(receiptRow(tradeId, createdBy, payTime, receiptNo, detailNo, accountNo,
                        amount, payerName, payee[1], bankName, payee[3]));
                statementBatch.add(statementRow(tradeId, createdBy, payTime, receiptNo, detailNo, accountNo,
                        amount, BigDecimal.ZERO, before, after));
                flowBatch.add(flowRow(tradeId, createdBy, payTime, "BF" + pad(tradeId, 12), accountNo, detailNo,
                        "debit", amount, before, after, "payment", payee[3]));
                tradeId++;
            }

            String orderStatus = agg == PayOrderStatusAgg.SUCCESS ? "success"
                    : agg == PayOrderStatusAgg.FAILED ? "failed" : "success";
            LocalDateTime payTime = "failed".equals(orderStatus) ? null : orderPayTime;
            lastEventTime = payTime != null ? payTime : applyTime;
            orderBatch.add(new Object[]{
                    (long) orderIndex, TENANT, createdBy, applyTime, applyTime, 0,
                    orderNo, accountNo, payerName, total, DETAILS_PER_ORDER, "CNY",
                    orderStatus, "采购及运营付款", "医院对公付款", applyTime, payTime,
                    "ebank", agg == PayOrderStatusAgg.FAILED ? "全部明细失败" : null
            });

            if (orderBatch.size() >= BATCH_SIZE || orderIndex == ORDER_COUNT) {
                flush(orderBatch, detailBatch, tradeBatch, receiptBatch, statementBatch, flowBatch);
                log.info("seed progress orders={}/{} tradesSoFar={}", orderIndex, ORDER_COUNT, tradeId - 1);
            }
        }

        insertCreditExtras(running, tradeId, lastEventTime, rng);
        log.info("seed done. successDetails={} failedDetails={}", successDetails, failedDetails);
    }

    private void wipeTenant() {
        jdbcTemplate.update("DELETE FROM electronic_statement WHERE tenant_id = ?", TENANT);
        jdbcTemplate.update("DELETE FROM electronic_receipt WHERE tenant_id = ?", TENANT);
        jdbcTemplate.update("DELETE FROM balance_flow WHERE tenant_id = ?", TENANT);
        jdbcTemplate.update("DELETE FROM trade_detail WHERE tenant_id = ?", TENANT);
        jdbcTemplate.update("DELETE FROM bank_pay_order_detail WHERE tenant_id = ?", TENANT);
        jdbcTemplate.update("DELETE FROM bank_pay_order WHERE tenant_id = ?", TENANT);
        jdbcTemplate.update("DELETE FROM bank_account WHERE tenant_id = ?", TENANT);
    }

    private BigDecimal[] insertAccounts() {
        LocalDateTime now = LocalDateTime.of(2024, 1, 8, 10, 0, 0);
        BigDecimal[] running = new BigDecimal[ACCOUNT_COUNT];
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < ACCOUNT_COUNT; i++) {
            String[] a = ACCOUNTS[i];
            BigDecimal balance = new BigDecimal(a[6]);
            running[i] = balance;
            rows.add(new Object[]{
                    (long) (i + 1), TENANT, "U1001", now, now, 0,
                    a[0], a[1], a[2], a[3], a[4], "CNY", a[5], "active",
                    balance, balance, BigDecimal.ZERO, LocalDate.of(2018, 5, 12), "医院资金账户"
            });
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO bank_account (id, tenant_id, created_by, created_at, updated_at, deleted,
                    account_no, account_name, bank_code, bank_name, branch_name, currency, account_type, status,
                    balance, available_balance, frozen_amount, open_date, remark)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, rows);
        return running;
    }

    private void insertCreditExtras(BigDecimal[] running, long startTradeId, LocalDateTime lastEventTime, Random rng) {
        LocalDateTime occurCursor = nextWorkdayMorning(lastEventTime, rng, 30);
        List<Object[]> trades = new ArrayList<>();
        List<Object[]> receipts = new ArrayList<>();
        List<Object[]> statements = new ArrayList<>();
        List<Object[]> flows = new ArrayList<>();
        long tradeId = startTradeId;
        for (int i = 0; i < CREDIT_EXTRA; i++) {
            int accountIdx = i % 2 == 0 ? 1 : 2;
            String[] account = ACCOUNTS[accountIdx];
            BigDecimal amount = new BigDecimal("200000.00");
            BigDecimal before = running[accountIdx];
            running[accountIdx] = before.add(amount);
            String detailNo = "TDX" + pad(i + 1, 8);
            String receiptNo = "RCX" + pad(i + 1, 8);
            LocalDateTime occur = occurCursor;
            occurCursor = advanceBusinessTime(occur, Duration.ofHours(2 + rng.nextInt(7)), rng);
            String createdBy = CREATORS[i % 2];
            trades.add(tradeRow(tradeId, createdBy, occur, detailNo, receiptNo, account[0],
                    "credit", amount, "102100000001", "市医保局", "人民银行", "医保基金拨付",
                    running[accountIdx], "BSX" + pad(tradeId, 12)));
            receipts.add(receiptRow(tradeId, createdBy, occur, receiptNo, detailNo, account[0],
                    amount, "市医保局", account[1], account[3], "医保基金拨付"));
            statements.add(statementRow(tradeId, createdBy, occur, receiptNo, detailNo, account[0],
                    BigDecimal.ZERO, amount, before, running[accountIdx]));
            flows.add(flowRow(tradeId, createdBy, occur, "BFX" + pad(tradeId, 12), account[0], detailNo,
                    "credit", amount, before, running[accountIdx], "inflow", "医保基金拨付"));
            tradeId++;
        }
        insertTrades(trades);
        insertReceipts(receipts);
        insertStatements(statements);
        insertFlows(flows);
    }

    private void flush(
            List<Object[]> orders,
            List<Object[]> details,
            List<Object[]> trades,
            List<Object[]> receipts,
            List<Object[]> statements,
            List<Object[]> flows) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO bank_pay_order (id, tenant_id, created_by, created_at, updated_at, deleted,
                    bank_pay_order_no, account_no, payer_name, total_amount, total_count, currency, status,
                    purpose, summary, apply_time, pay_time, channel, fail_reason)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, orders);
        jdbcTemplate.batchUpdate("""
                INSERT INTO bank_pay_order_detail (id, tenant_id, created_by, created_at, updated_at, deleted,
                    bank_pay_order_id, bank_pay_order_no, bank_pay_order_detail_no, account_no,
                    payee_account_no, payee_name, payee_bank_name, amount, currency, status,
                    usage_desc, seq_no, fail_reason)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, details);
        insertTrades(trades);
        insertReceipts(receipts);
        insertStatements(statements);
        insertFlows(flows);
        orders.clear();
        details.clear();
        trades.clear();
        receipts.clear();
        statements.clear();
        flows.clear();
    }

    private void insertTrades(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO trade_detail (id, tenant_id, created_by, created_at, updated_at, deleted,
                    trade_detail_no, receipt_no, account_no, direction, amount, currency,
                    counterpart_account_no, counterpart_name, counterpart_bank_name, summary,
                    trade_time, value_date, balance_after, bank_serial_no)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, rows);
    }

    private void insertReceipts(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO electronic_receipt (id, tenant_id, created_by, created_at, updated_at, deleted,
                    receipt_no, trade_detail_no, account_no, amount, currency, payer_name, payee_name,
                    issue_time, bank_name, digest)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, rows);
    }

    private void insertStatements(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO electronic_statement (id, tenant_id, created_by, created_at, updated_at, deleted,
                    statement_no, trade_detail_no, account_no, period_start, period_end,
                    opening_balance, closing_balance, debit_total, credit_total, status, issue_time)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, rows);
    }

    private void insertFlows(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO balance_flow (id, tenant_id, created_by, created_at, updated_at, deleted,
                    flow_no, account_no, trade_detail_no, direction, change_amount, balance_before, balance_after,
                    occur_time, biz_type, summary)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, rows);
    }

    private static Object[] tradeRow(
            long id, String createdBy, LocalDateTime time, String tradeNo, String receiptNo, String accountNo,
            String direction, BigDecimal amount, String cpAcc, String cpName, String cpBank, String summary,
            BigDecimal after, String serial) {
        return new Object[]{
                id, TENANT, createdBy, time, time, 0,
                tradeNo, receiptNo, accountNo, direction, amount, "CNY",
                cpAcc, cpName, cpBank, summary, time, time.toLocalDate(), after, serial
        };
    }

    private static Object[] receiptRow(
            long id, String createdBy, LocalDateTime time, String receiptNo, String tradeNo, String accountNo,
            BigDecimal amount, String payer, String payee, String bankName, String digest) {
        return new Object[]{
                id, TENANT, createdBy, time, time, 0,
                receiptNo, tradeNo, accountNo, amount, "CNY", payer, payee, time, bankName, digest
        };
    }

    private static Object[] statementRow(
            long id, String createdBy, LocalDateTime time, String statementNo, String tradeNo, String accountNo,
            BigDecimal debit, BigDecimal credit, BigDecimal opening, BigDecimal closing) {
        return new Object[]{
                id, TENANT, createdBy, time, time, 0,
                statementNo, tradeNo, accountNo, time.toLocalDate(), time.toLocalDate(),
                opening, closing, debit, credit, "issued", time
        };
    }

    private static Object[] flowRow(
            long id, String createdBy, LocalDateTime time, String flowNo, String accountNo, String tradeNo,
            String direction, BigDecimal amount, BigDecimal before, BigDecimal after, String bizType, String summary) {
        return new Object[]{
                id, TENANT, createdBy, time, time, 0,
                flowNo, accountNo, tradeNo, direction, amount, before, after, time, bizType, summary
        };
    }

    private static String pad(long value, int width) {
        return String.format("%0" + width + "d", value);
    }

    private static LocalDateTime nextBusinessApplyTime(LocalDateTime cursor, Random rng) {
        int gapSeconds = 30 + rng.nextInt(8 * 60 - 30 + 1);
        LocalDateTime candidate = cursor.plusSeconds(gapSeconds + rng.nextInt(60));
        if (notInWorkWindow(candidate)) {
            return nextWorkdayMorning(cursor, rng, 30);
        }
        return candidate;
    }

    private static LocalDateTime randomPayTime(LocalDateTime applyTime, Random rng) {
        LocalDateTime pay = applyTime.plusMinutes(5 + rng.nextInt(86));
        if (notInWorkWindow(pay)) {
            return nextWorkdayMorning(applyTime, rng, 60);
        }
        return pay;
    }

    private static LocalDateTime advanceBusinessTime(LocalDateTime from, Duration delta, Random rng) {
        LocalDateTime next = from.plus(delta);
        if (notInWorkWindow(next)) {
            return nextWorkdayMorning(from, rng, 30);
        }
        return next;
    }

    private static LocalDateTime nextWorkdayMorning(LocalDateTime from, Random rng, int maxOffsetMinutes) {
        LocalDate date = from.toLocalDate().plusDays(1);
        while (isWeekend(date)) {
            date = date.plusDays(1);
        }
        int offsetMinutes = maxOffsetMinutes <= 0 ? 0 : rng.nextInt(maxOffsetMinutes + 1);
        return date.atTime(WORK_START_HOUR, 0).plusMinutes(offsetMinutes).plusSeconds(rng.nextInt(60));
    }

    private static boolean notInWorkWindow(LocalDateTime time) {
        if (isWeekend(time.toLocalDate())) {
            return true;
        }
        int hour = time.getHour();
        return hour < WORK_START_HOUR || hour >= WORK_END_HOUR;
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private enum PayOrderStatusAgg {
        SUCCESS, FAILED, PARTIAL
    }
}
