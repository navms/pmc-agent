package io.github.navms.infrastructure.converter;

import io.github.navms.domain.bank.entity.BankPayOrder;
import io.github.navms.domain.bank.enums.PayOrderStatus;
import io.github.navms.infrastructure.model.BankPayOrderDO;
import io.github.navms.utils.enums.EnumConverter;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 支付单 DO ↔ Domain。
 *
 * @author navms
 */
@Mapper(uses = EnumConverter.class)
public interface BankPayOrderConverter {

    BankPayOrderConverter INSTANCE = Mappers.getMapper(BankPayOrderConverter.class);

    /**
     * @param source 领域对象
     * @return DO
     */
    BankPayOrderDO toBankPayOrderDO(BankPayOrder source);

    /**
     * @param source DO
     * @return 领域对象
     */
    default BankPayOrder toBankPayOrder(BankPayOrderDO source) {
        if (source == null) {
            return null;
        }
        EnumConverter enums = new EnumConverter();
        return new BankPayOrder.Builder(source.getTenantId(), source.getBankPayOrderNo(), source.getAccountNo())
                .id(source.getId())
                .createdBy(source.getCreatedBy())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .payerName(source.getPayerName())
                .totalAmount(source.getTotalAmount())
                .totalCount(source.getTotalCount())
                .currency(source.getCurrency())
                .status(enums.string2Enum(source.getStatus(), PayOrderStatus.class))
                .purpose(source.getPurpose())
                .summary(source.getSummary())
                .applyTime(source.getApplyTime())
                .payTime(source.getPayTime())
                .channel(source.getChannel())
                .failReason(source.getFailReason())
                .build();
    }

    /**
     * @param source DO 列表
     * @return 领域列表
     */
    List<BankPayOrder> toBankPayOrderList(List<BankPayOrderDO> source);
}
