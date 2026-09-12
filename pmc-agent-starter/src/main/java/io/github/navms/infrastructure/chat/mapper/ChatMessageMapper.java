package io.github.navms.infrastructure.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.navms.infrastructure.model.ChatMessageDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息 Mapper。
 *
 * @author navms
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageDO> {
}
