package io.github.navms.bank.api;

import io.github.navms.bank.api.dto.BankPayOrderSubmitInfo;
import io.github.navms.bank.api.dto.BankSyncInfo;
import io.github.navms.bank.api.dto.BankWriteResult;
import io.github.navms.bank.api.dto.SubmitBankPayOrderCommand;
import io.github.navms.bank.api.dto.SyncBankDataCommand;

/**
 * 银企直连写操作 API：支付提交与从银行拉数同步。
 *
 * @author navms
 */
public interface BankWriteApi {

    /**
     * @param command 提交支付单
     * @return 提交结果
     */
    BankWriteResult<BankPayOrderSubmitInfo> submitBankPayOrder(SubmitBankPayOrderCommand command);

    /**
     * @param command 同步条件
     * @return 同步结果
     */
    BankWriteResult<BankSyncInfo> syncTradeDetails(SyncBankDataCommand command);

    /**
     * @param command 同步条件
     * @return 同步结果
     */
    BankWriteResult<BankSyncInfo> syncBalanceFlows(SyncBankDataCommand command);

    /**
     * @param command 同步条件
     * @return 同步结果
     */
    BankWriteResult<BankSyncInfo> syncElectronicReceipts(SyncBankDataCommand command);

    /**
     * @param command 同步条件
     * @return 同步结果
     */
    BankWriteResult<BankSyncInfo> syncElectronicStatements(SyncBankDataCommand command);
}
