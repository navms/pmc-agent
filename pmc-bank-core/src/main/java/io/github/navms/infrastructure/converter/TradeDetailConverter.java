package io.github.navms.infrastructure.converter;

import io.github.navms.domain.bank.entity.TradeDetail;
import io.github.navms.domain.bank.enums.TradeDirection;
import io.github.navms.infrastructure.model.TradeDetailDO;
import io.github.navms.utils.enums.EnumConverter;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 交易明细 DO ↔ Domain。
 *
 * @author navms
 */
@Mapper(uses = EnumConverter.class)
public interface TradeDetailConverter {

    TradeDetailConverter INSTANCE = Mappers.getMapper(TradeDetailConverter.class);

    /**
     * @param source 领域对象
     * @return DO
     */
    TradeDetailDO toTradeDetailDO(TradeDetail source);

    /**
     * @param source DO
     * @return 领域对象
     */
    default TradeDetail toTradeDetail(TradeDetailDO source) {
        if (source == null) {
            return null;
        }
        EnumConverter enums = new EnumConverter();
        return new TradeDetail.Builder(source.getTenantId(), source.getTradeDetailNo(), source.getAccountNo())
                .id(source.getId())
                .createdBy(source.getCreatedBy())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .receiptNo(source.getReceiptNo())
                .direction(enums.string2Enum(source.getDirection(), TradeDirection.class))
                .amount(source.getAmount())
                .currency(source.getCurrency())
                .counterpartAccountNo(source.getCounterpartAccountNo())
                .counterpartName(source.getCounterpartName())
                .counterpartBankName(source.getCounterpartBankName())
                .summary(source.getSummary())
                .tradeTime(source.getTradeTime())
                .valueDate(source.getValueDate())
                .balanceAfter(source.getBalanceAfter())
                .bankSerialNo(source.getBankSerialNo())
                .build();
    }

    /**
     * @param source DO 列表
     * @return 领域列表
     */
    List<TradeDetail> toTradeDetailList(List<TradeDetailDO> source);
}
