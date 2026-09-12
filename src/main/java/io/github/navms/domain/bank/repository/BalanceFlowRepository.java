package io.github.navms.domain.bank.repository;

import io.github.navms.domain.bank.entity.BalanceFlow;

import java.time.LocalDate;
import java.util.List;

/**
 * 余额流水查询仓储。
 *
 * @author navms
 */
public interface BalanceFlowRepository {

    /**
     * @param tenantId  租户
     * @param accountNo 账号，可空
     * @param startDate 发生日起，可空
     * @param endDate   发生日止，可空
     * @return 流水列表
     */
    List<BalanceFlow> query(String tenantId, String accountNo, LocalDate startDate, LocalDate endDate);
}
