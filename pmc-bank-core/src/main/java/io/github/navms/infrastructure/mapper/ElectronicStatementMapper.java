package io.github.navms.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.navms.infrastructure.model.AggregateRowDO;
import io.github.navms.infrastructure.model.ElectronicStatementDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 电子对账单 Mapper。
 *
 * @author navms
 */
@Mapper
public interface ElectronicStatementMapper extends BaseMapper<ElectronicStatementDO> {

    /**
     * @param tenantId  租户
     * @param accountNo 账号
     * @param status    状态 code
     * @param startTime 出具时间起
     * @param endTime   出具时间止（不含）
     * @param groupExpr 白名单分组表达式
     * @param limit     最大行
     * @return 聚合行，金额为借贷发生额合计
     */
    List<AggregateRowDO> aggregate(
            @Param("tenantId") String tenantId,
            @Param("accountNo") String accountNo,
            @Param("status") String status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("groupExpr") String groupExpr,
            @Param("limit") int limit);
}
