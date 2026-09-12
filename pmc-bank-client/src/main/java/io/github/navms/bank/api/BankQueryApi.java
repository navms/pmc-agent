package io.github.navms.bank.api;

import io.github.navms.bank.api.dto.AccountInfo;
import io.github.navms.bank.api.dto.AccountQuery;
import io.github.navms.bank.api.dto.BalanceFlowInfo;
import io.github.navms.bank.api.dto.BalanceFlowQuery;
import io.github.navms.bank.api.dto.BankPayOrderDetailInfo;
import io.github.navms.bank.api.dto.BankPayOrderDetailQuery;
import io.github.navms.bank.api.dto.BankPayOrderInfo;
import io.github.navms.bank.api.dto.BankPayOrderQuery;
import io.github.navms.bank.api.dto.BankQueryResult;
import io.github.navms.bank.api.dto.ElectronicReceiptInfo;
import io.github.navms.bank.api.dto.ElectronicReceiptQuery;
import io.github.navms.bank.api.dto.ElectronicStatementInfo;
import io.github.navms.bank.api.dto.ElectronicStatementQuery;
import io.github.navms.bank.api.dto.TradeDetailInfo;
import io.github.navms.bank.api.dto.TradeDetailQuery;

/**
 * 银企直连只读查询 API。
 *
 * @author navms
 */
public interface BankQueryApi {

    /**
     * @param query 查询
     * @return 账户
     */
    BankQueryResult<AccountInfo> queryAccounts(AccountQuery query);

    /**
     * @param query 查询
     * @return 支付单
     */
    BankQueryResult<BankPayOrderInfo> queryBankPayOrders(BankPayOrderQuery query);

    /**
     * @param query 查询
     * @return 支付明细
     */
    BankQueryResult<BankPayOrderDetailInfo> queryBankPayOrderDetails(BankPayOrderDetailQuery query);

    /**
     * @param query 查询
     * @return 交易明细
     */
    BankQueryResult<TradeDetailInfo> queryTradeDetails(TradeDetailQuery query);

    /**
     * @param query 查询
     * @return 电子回单
     */
    BankQueryResult<ElectronicReceiptInfo> queryElectronicReceipts(ElectronicReceiptQuery query);

    /**
     * @param query 查询
     * @return 余额流水
     */
    BankQueryResult<BalanceFlowInfo> queryBalanceFlows(BalanceFlowQuery query);

    /**
     * @param query 查询
     * @return 电子对账单
     */
    BankQueryResult<ElectronicStatementInfo> queryElectronicStatements(ElectronicStatementQuery query);
}
