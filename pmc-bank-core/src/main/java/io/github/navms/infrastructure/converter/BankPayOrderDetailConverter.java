package io.github.navms.infrastructure.converter;

import io.github.navms.domain.bank.entity.BankPayOrderDetail;
import io.github.navms.domain.bank.enums.PayOrderStatus;
import io.github.navms.infrastructure.model.BankPayOrderDetailDO;
import io.github.navms.utils.enums.EnumConverter;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 支付明细 DO ↔ Domain。
 *
 * @author navms
 */
@Mapper(uses = EnumConverter.class)
public interface BankPayOrderDetailConverter {

    BankPayOrderDetailConverter INSTANCE = Mappers.getMapper(BankPayOrderDetailConverter.class);

    /**
     * @param source DO
     * @return 领域对象
     */
    default BankPayOrderDetail toBankPayOrderDetail(BankPayOrderDetailDO source) {
        if (source == null) {
            return null;
        }
        EnumConverter enums = new EnumConverter();
        return new BankPayOrderDetail.Builder(
                source.getTenantId(),
                source.getBankPayOrderId(),
                source.getBankPayOrderNo(),
                source.getBankPayOrderDetailNo(),
                source.getAccountNo())
                .id(source.getId())
                .createdBy(source.getCreatedBy())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .payeeAccountNo(source.getPayeeAccountNo())
                .payeeName(source.getPayeeName())
                .payeeBankName(source.getPayeeBankName())
                .amount(source.getAmount())
                .currency(source.getCurrency())
                .status(enums.string2Enum(source.getStatus(), PayOrderStatus.class))
                .usage(source.getUsageDesc())
                .seqNo(source.getSeqNo())
                .failReason(source.getFailReason())
                .build();
    }

    /**
     * @param source DO 列表
     * @return 领域列表
     */
    List<BankPayOrderDetail> toBankPayOrderDetailList(List<BankPayOrderDetailDO> source);
}
