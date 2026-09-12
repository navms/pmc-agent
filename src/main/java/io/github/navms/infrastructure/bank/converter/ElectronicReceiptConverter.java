package io.github.navms.infrastructure.bank.converter;

import io.github.navms.domain.bank.entity.ElectronicReceipt;
import io.github.navms.infrastructure.bank.model.ElectronicReceiptDO;
import io.github.navms.utils.enums.EnumConverter;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 电子回单 DO ↔ Domain。
 *
 * @author navms
 */
@Mapper(uses = EnumConverter.class)
public interface ElectronicReceiptConverter {

    ElectronicReceiptConverter INSTANCE = Mappers.getMapper(ElectronicReceiptConverter.class);

    /**
     * @param source 领域对象
     * @return DO
     */
    ElectronicReceiptDO toElectronicReceiptDO(ElectronicReceipt source);

    /**
     * @param source DO
     * @return 领域对象
     */
    default ElectronicReceipt toElectronicReceipt(ElectronicReceiptDO source) {
        if (source == null) {
            return null;
        }
        return new ElectronicReceipt.Builder(
                source.getTenantId(), source.getReceiptNo(), source.getTradeDetailNo(), source.getAccountNo())
                .id(source.getId())
                .createdBy(source.getCreatedBy())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .amount(source.getAmount())
                .currency(source.getCurrency())
                .payerName(source.getPayerName())
                .payeeName(source.getPayeeName())
                .issueTime(source.getIssueTime())
                .bankName(source.getBankName())
                .digest(source.getDigest())
                .build();
    }

    /**
     * @param source DO 列表
     * @return 领域列表
     */
    List<ElectronicReceipt> toElectronicReceiptList(List<ElectronicReceiptDO> source);
}
