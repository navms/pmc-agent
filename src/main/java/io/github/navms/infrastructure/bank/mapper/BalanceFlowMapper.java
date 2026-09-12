package io.github.navms.infrastructure.bank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.navms.infrastructure.bank.model.BalanceFlowDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 余额流水 Mapper。
 *
 * @author navms
 */
@Mapper
public interface BalanceFlowMapper extends BaseMapper<BalanceFlowDO> {
}
