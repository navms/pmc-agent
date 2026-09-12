package io.github.navms.infrastructure.bank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.navms.infrastructure.bank.model.BankAggregateRowDO;
import io.github.navms.infrastructure.bank.model.TradeDetailDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易明细 Mapper。
 *
 * @author navms
 */
@Mapper
public interface TradeDetailMapper extends BaseMapper<TradeDetailDO> {

    /**
     * @param tenantId  租户
     * @param accountNo 账号
     * @param direction 借贷方向 code
     * @param startTime 交易时间起
     * @param endTime   交易时间止（不含）
     * @param groupExpr 白名单分组表达式
     * @param limit     最大行
     * @return 聚合行
     */
    List<BankAggregateRowDO> aggregate(
            @Param("tenantId") String tenantId,
            @Param("accountNo") String accountNo,
            @Param("direction") String direction,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("groupExpr") String groupExpr,
            @Param("limit") int limit);
}
