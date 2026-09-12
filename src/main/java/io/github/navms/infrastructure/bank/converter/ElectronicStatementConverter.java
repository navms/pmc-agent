package io.github.navms.infrastructure.bank.converter;

import io.github.navms.domain.bank.entity.ElectronicStatement;
import io.github.navms.domain.bank.enums.StatementStatus;
import io.github.navms.infrastructure.bank.model.ElectronicStatementDO;
import io.github.navms.utils.enums.EnumConverter;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 电子对账单 DO ↔ Domain。
 *
 * @author navms
 */
@Mapper(uses = EnumConverter.class)
public interface ElectronicStatementConverter {

    ElectronicStatementConverter INSTANCE = Mappers.getMapper(ElectronicStatementConverter.class);

    /**
     * @param source 领域对象
     * @return DO
     */
    ElectronicStatementDO toElectronicStatementDO(ElectronicStatement source);

    /**
     * @param source DO
     * @return 领域对象
     */
    default ElectronicStatement toElectronicStatement(ElectronicStatementDO source) {
        if (source == null) {
            return null;
        }
        EnumConverter enums = new EnumConverter();
        return new ElectronicStatement.Builder(
                source.getTenantId(), source.getStatementNo(), source.getTradeDetailNo(), source.getAccountNo())
                .id(source.getId())
                .createdBy(source.getCreatedBy())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .periodStart(source.getPeriodStart())
                .periodEnd(source.getPeriodEnd())
                .openingBalance(source.getOpeningBalance())
                .closingBalance(source.getClosingBalance())
                .debitTotal(source.getDebitTotal())
                .creditTotal(source.getCreditTotal())
                .status(enums.string2Enum(source.getStatus(), StatementStatus.class))
                .issueTime(source.getIssueTime())
                .build();
    }

    /**
     * @param source DO 列表
     * @return 领域列表
     */
    List<ElectronicStatement> toElectronicStatementList(List<ElectronicStatementDO> source);
}
