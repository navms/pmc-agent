package io.github.navms.infrastructure.chat.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话表。
 *
 * @author navms
 */
@Data
@TableName("chat_session")
public class ChatSessionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private String title;

    private String status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
