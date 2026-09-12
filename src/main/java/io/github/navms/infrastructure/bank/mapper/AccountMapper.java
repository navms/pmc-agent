package io.github.navms.infrastructure.bank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.navms.infrastructure.bank.model.AccountDO;
import io.github.navms.infrastructure.bank.model.BankAggregateRowDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 账户 Mapper。
 *
 * @author navms
 */
@Mapper
public interface AccountMapper extends BaseMapper<AccountDO> {

    /**
     * @param tenantId  租户
     * @param accountNo 账号
     * @param status    状态 code
     * @param groupExpr 白名单分组表达式
     * @param limit     最大行
     * @return 聚合行，金额为余额合计
     */
    List<BankAggregateRowDO> aggregate(
            @Param("tenantId") String tenantId,
            @Param("accountNo") String accountNo,
            @Param("status") String status,
            @Param("groupExpr") String groupExpr,
            @Param("limit") int limit);
}
