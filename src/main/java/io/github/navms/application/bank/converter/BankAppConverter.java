package io.github.navms.application.bank.converter;

import io.github.navms.application.bank.dto.AccountInfo;
import io.github.navms.application.bank.dto.BankAggregateInfo;
import io.github.navms.application.bank.dto.BalanceFlowInfo;
import io.github.navms.application.bank.dto.BankPayOrderDetailInfo;
import io.github.navms.application.bank.dto.BankPayOrderInfo;
import io.github.navms.application.bank.dto.ElectronicReceiptInfo;
import io.github.navms.application.bank.dto.ElectronicStatementInfo;
import io.github.navms.application.bank.dto.TradeDetailInfo;
import io.github.navms.domain.bank.entity.Account;
import io.github.navms.domain.bank.entity.BalanceFlow;
import io.github.navms.domain.bank.entity.BankPayOrder;
import io.github.navms.domain.bank.entity.BankPayOrderDetail;
import io.github.navms.domain.bank.entity.ElectronicReceipt;
import io.github.navms.domain.bank.entity.ElectronicStatement;
import io.github.navms.domain.bank.entity.TradeDetail;
import io.github.navms.domain.bank.valueobj.BankAggregateRow;
import io.github.navms.utils.enums.EnumConverter;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 银企领域对象到 Info。
 *
 * @author navms
 */
@Mapper(uses = EnumConverter.class)
public interface BankAppConverter {

    BankAppConverter INSTANCE = Mappers.getMapper(BankAppConverter.class);

    /**
     * @param source 账户
     * @return Info
     */
    AccountInfo toAccountInfo(Account source);

    /**
     * @param source 账户列表
     * @return Info 列表
     */
    List<AccountInfo> toAccountInfoList(List<Account> source);

    /**
     * @param source 支付单
     * @return Info
     */
    BankPayOrderInfo toBankPayOrderInfo(BankPayOrder source);

    /**
     * @param source 支付单列表
     * @return Info 列表
     */
    List<BankPayOrderInfo> toBankPayOrderInfoList(List<BankPayOrder> source);

    /**
     * @param source 支付明细
     * @return Info
     */
    BankPayOrderDetailInfo toBankPayOrderDetailInfo(BankPayOrderDetail source);

    /**
     * @param source 支付明细列表
     * @return Info 列表
     */
    List<BankPayOrderDetailInfo> toBankPayOrderDetailInfoList(List<BankPayOrderDetail> source);

    /**
     * @param source 交易明细
     * @return Info
     */
    TradeDetailInfo toTradeDetailInfo(TradeDetail source);

    /**
     * @param source 交易明细列表
     * @return Info 列表
     */
    List<TradeDetailInfo> toTradeDetailInfoList(List<TradeDetail> source);

    /**
     * @param source 回单
     * @return Info
     */
    ElectronicReceiptInfo toElectronicReceiptInfo(ElectronicReceipt source);

    /**
     * @param source 回单列表
     * @return Info 列表
     */
    List<ElectronicReceiptInfo> toElectronicReceiptInfoList(List<ElectronicReceipt> source);

    /**
     * @param source 余额流水
     * @return Info
     */
    BalanceFlowInfo toBalanceFlowInfo(BalanceFlow source);

    /**
     * @param source 余额流水列表
     * @return Info 列表
     */
    List<BalanceFlowInfo> toBalanceFlowInfoList(List<BalanceFlow> source);

    /**
     * @param source 对账单
     * @return Info
     */
    ElectronicStatementInfo toElectronicStatementInfo(ElectronicStatement source);

    /**
     * @param source 对账单列表
     * @return Info 列表
     */
    List<ElectronicStatementInfo> toElectronicStatementInfoList(List<ElectronicStatement> source);

    /**
     * @param source 聚合行
     * @return Info
     */
    BankAggregateInfo toBankAggregateInfo(BankAggregateRow source);

    /**
     * @param source 聚合行列表
     * @return Info 列表
     */
    List<BankAggregateInfo> toBankAggregateInfoList(List<BankAggregateRow> source);
}
