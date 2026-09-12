package io.github.navms.domain.chat.entity;

import io.github.navms.domain.chat.enums.ChatSessionStatus;
import io.github.navms.domain.chat.exception.BusinessException;
import io.github.navms.domain.chat.exception.ChatErrorCode;
import io.github.navms.domain.chat.valueobj.SessionTitle;
import io.github.navms.domain.chat.valueobj.UserId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 聊天会话聚合根。
 *
 * @author navms
 */
@Getter
public class ChatSession {

    private Long id;

    private final UserId userId;

    private SessionTitle title;

    private ChatSessionStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer deleted;

    private ChatSession(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.title = builder.title;
        this.status = builder.status == null ? ChatSessionStatus.ACTIVE : builder.status;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.deleted = builder.deleted == null ? 0 : builder.deleted;
    }

    /**
     * 新建会话。
     *
     * @param userId 用户
     * @return 会话
     */
    public static ChatSession create(UserId userId) {
        LocalDateTime now = LocalDateTime.now();
        return new Builder(userId)
                .status(ChatSessionStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .deleted(0)
                .build();
    }

    /**
     * 更新标题。
     *
     * @param title 新标题
     */
    public void rename(SessionTitle title) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ChatErrorCode.TITLE_REQUIRED);
        }
        this.title = title;
        touch();
    }

    /**
     * 首条用户消息时用提问内容填充空标题。
     *
     * @param prompt 用户输入
     */
    public void applyFirstUserPrompt(String prompt) {
        if (title == null || title.isBlank()) {
            this.title = SessionTitle.fromPrompt(prompt);
            touch();
        }
    }

    /**
     * 标记为可继续对话。
     */
    public void markActive() {
        this.status = ChatSessionStatus.ACTIVE;
        touch();
    }

    /**
     * 标记为等待人工确认。
     */
    public void markInterrupted() {
        this.status = ChatSessionStatus.INTERRUPTED;
        touch();
    }

    /**
     * 刷新更新时间（追加消息时）。
     */
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 会话构建器。
     *
     * @author navms
     */
    public static final class Builder {

        private Long id;

        private final UserId userId;

        private SessionTitle title;

        private ChatSessionStatus status;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;

        private Integer deleted;

        /**
         * @param userId 用户，必填
         */
        public Builder(UserId userId) {
            if (userId == null) {
                throw new IllegalArgumentException("userId cannot be null");
            }
            this.userId = userId;
        }

        /**
         * @param id 主键
         * @return this
         */
        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * @param title 标题
         * @return this
         */
        public Builder title(SessionTitle title) {
            this.title = title;
            return this;
        }

        /**
         * @param status 状态
         * @return this
         */
        public Builder status(ChatSessionStatus status) {
            this.status = status;
            return this;
        }

        /**
         * @param createdAt 创建时间
         * @return this
         */
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @param updatedAt 更新时间
         * @return this
         */
        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        /**
         * @param deleted 逻辑删除标记
         * @return this
         */
        public Builder deleted(Integer deleted) {
            this.deleted = deleted;
            return this;
        }

        /**
         * @return 会话
         */
        public ChatSession build() {
            return new ChatSession(this);
        }
    }
}
