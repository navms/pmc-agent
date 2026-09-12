package io.github.navms.infrastructure.converter;

import io.github.navms.domain.bank.entity.Account;
import io.github.navms.domain.bank.enums.AccountStatus;
import io.github.navms.domain.bank.enums.AccountType;
import io.github.navms.infrastructure.model.AccountDO;
import io.github.navms.utils.enums.BaseEnum;
import io.github.navms.utils.enums.EnumConverter;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 账户 DO ↔ Domain。
 *
 * @author navms
 */
@Mapper(uses = EnumConverter.class)
public interface AccountConverter {

    AccountConverter INSTANCE = Mappers.getMapper(AccountConverter.class);

    /**
     * @param source DO
     * @return 领域对象
     */
    default Account toAccount(AccountDO source) {
        if (source == null) {
            return null;
        }
        return new Account.Builder(source.getTenantId(), source.getAccountNo())
                .id(source.getId())
                .createdBy(source.getCreatedBy())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .balance(source.getBalance())
                .status(BaseEnum.of(AccountStatus.class, source.getStatus()))
                .bankCode(source.getBankCode())
                .bankName(source.getBankName())
                .branchName(source.getBranchName())
                .availableBalance(source.getAvailableBalance())
                .frozenAmount(source.getFrozenAmount())
                .openDate(source.getOpenDate())
                .remark(source.getRemark())
                .accountType(BaseEnum.of(AccountType.class, source.getAccountType()))
                .build();
    }

    /**
     * @param source DO 列表
     * @return 领域列表
     */
    List<Account> toAccountList(List<AccountDO> source);
}
