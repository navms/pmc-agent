package io.github.navms.infrastructure.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.navms.infrastructure.chat.model.ChatSessionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话 Mapper。
 *
 * @author navms
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSessionDO> {
}
