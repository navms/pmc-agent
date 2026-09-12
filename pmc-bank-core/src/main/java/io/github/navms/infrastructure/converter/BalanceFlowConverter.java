package io.github.navms.infrastructure.converter;

import io.github.navms.domain.bank.entity.BalanceFlow;
import io.github.navms.domain.bank.enums.TradeDirection;
import io.github.navms.infrastructure.model.BalanceFlowDO;
import io.github.navms.utils.enums.EnumConverter;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 余额流水 DO ↔ Domain。
 *
 * @author navms
 */
@Mapper(uses = EnumConverter.class)
public interface BalanceFlowConverter {

    BalanceFlowConverter INSTANCE = Mappers.getMapper(BalanceFlowConverter.class);

    /**
     * @param source 领域对象
     * @return DO
     */
    BalanceFlowDO toBalanceFlowDO(BalanceFlow source);

    /**
     * @param source DO
     * @return 领域对象
     */
    default BalanceFlow toBalanceFlow(BalanceFlowDO source) {
        if (source == null) {
            return null;
        }
        EnumConverter enums = new EnumConverter();
        return new BalanceFlow.Builder(source.getTenantId(), source.getFlowNo(), source.getAccountNo())
                .id(source.getId())
                .createdBy(source.getCreatedBy())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .tradeDetailNo(source.getTradeDetailNo())
                .direction(enums.string2Enum(source.getDirection(), TradeDirection.class))
                .changeAmount(source.getChangeAmount())
                .balanceBefore(source.getBalanceBefore())
                .balanceAfter(source.getBalanceAfter())
                .occurTime(source.getOccurTime())
                .bizType(source.getBizType())
                .summary(source.getSummary())
                .build();
    }

    /**
     * @param source DO 列表
     * @return 领域列表
     */
    List<BalanceFlow> toBalanceFlowList(List<BalanceFlowDO> source);
}
