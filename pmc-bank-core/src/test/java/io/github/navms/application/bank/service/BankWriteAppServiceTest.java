package io.github.navms.application.bank.service;

import io.github.navms.bank.api.dto.BankPayOrderSubmitInfo;
import io.github.navms.bank.api.dto.BankSyncInfo;
import io.github.navms.bank.api.dto.BankWriteResult;
import io.github.navms.bank.api.dto.SubmitBankPayOrderCommand;
import io.github.navms.bank.api.dto.SyncBankDataCommand;
import io.github.navms.domain.bank.Defaults;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankWriteAppServiceTest {

    private final BankWriteAppService service = new BankWriteAppService();

    @Test
    void submitUsesDefaultTenantAndMockFields() {
        BankWriteResult<BankPayOrderSubmitInfo> result = service.submitBankPayOrder(
                new SubmitBankPayOrderCommand(
                        null,
                        null,
                        "6222021000000001001",
                        new BigDecimal("100.50"),
                        null,
                        "药品采购",
                        "mock"));
        assertTrue(result.success());
        assertNotNull(result.data());
        assertEquals(Defaults.DEFAULT_TENANT_ID, result.data().tenantId());
        assertTrue(result.data().mocked());
        assertTrue(result.data().bankPayOrderNo().startsWith("PO"));
        assertEquals("submitted", result.data().status());
        assertTrue(result.data().bankAcceptNo().startsWith("MOCK-BA-"));
        assertEquals(1, result.data().totalCount());
        assertEquals("药品采购", result.data().purpose());
    }

    @Test
    void submitKeepsProvidedOrderNo() {
        BankWriteResult<BankPayOrderSubmitInfo> result = service.submitBankPayOrder(
                new SubmitBankPayOrderCommand(
                        "T009",
                        "PO-EXISTING",
                        "6222021000000001001",
                        new BigDecimal("1"),
                        3,
                        null,
                        null));
        assertTrue(result.success());
        assertEquals("T009", result.data().tenantId());
        assertEquals("PO-EXISTING", result.data().bankPayOrderNo());
        assertEquals(3, result.data().totalCount());
    }

    @Test
    void submitRejectsMissingAccountOrAmount() {
        assertFalse(service.submitBankPayOrder(
                new SubmitBankPayOrderCommand(null, null, " ", BigDecimal.ONE, null, null, null)).success());
        assertFalse(service.submitBankPayOrder(
                new SubmitBankPayOrderCommand(null, null, "acc", BigDecimal.ZERO, null, null, null)).success());
    }

    @Test
    void syncPassesDatesAndDefaultTenant() {
        BankWriteResult<BankSyncInfo> result = service.syncTradeDetails(
                new SyncBankDataCommand(null, "6222021000000001001", "2026-09-01", "2026-09-11"));
        assertTrue(result.success());
        assertEquals(Defaults.DEFAULT_TENANT_ID, result.data().tenantId());
        assertTrue(result.data().mocked());
        assertEquals("trade_detail", result.data().dataType());
        assertEquals("2026-09-01", result.data().startDate());
        assertEquals("2026-09-11", result.data().endDate());
        assertTrue(result.data().syncedCount() >= 3);
        assertTrue(result.data().syncedCount() <= 14);
    }

    @Test
    void syncRejectsBadDateAndMissingAccount() {
        assertFalse(service.syncBalanceFlows(
                new SyncBankDataCommand(null, "acc", "2026/09/01", "2026-09-11")).success());
        BankWriteResult<BankSyncInfo> missing = service.syncElectronicReceipts(
                new SyncBankDataCommand("T001", null, "2026-09-01", "2026-09-11"));
        assertFalse(missing.success());
        assertNull(missing.data());
        BankWriteResult<BankSyncInfo> statements = service.syncElectronicStatements(
                new SyncBankDataCommand("T001", "acc", null, null));
        assertTrue(statements.success());
        assertEquals("electronic_statement", statements.data().dataType());
        assertNull(statements.data().startDate());
        assertNull(statements.data().endDate());
    }
}
