package io.github.navms.infrastructure.chat.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 会话消息表。
 *
 * @author navms
 */
@Data
@TableName(value = "chat_message", autoResultMap = true)
public class ChatMessageDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String messageType;

    private String content;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> payload;

    private Integer seq;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

}
