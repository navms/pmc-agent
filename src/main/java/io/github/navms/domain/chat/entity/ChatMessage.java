package io.github.navms.domain.chat.entity;

import io.github.navms.domain.chat.enums.ChatMessageType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 会话消息。
 *
 * @author navms
 */
@Getter
public class ChatMessage {

    private Long id;

    private final Long sessionId;

    private final ChatMessageType messageType;

    private final String content;

    private final Map<String, Object> payload;

    private final Integer seq;

    private final LocalDateTime createdAt;

    private final Integer deleted;

    private ChatMessage(Builder builder) {
        this.id = builder.id;
        this.sessionId = builder.sessionId;
        this.messageType = builder.messageType;
        this.content = builder.content == null ? "" : builder.content;
        this.payload = builder.payload;
        this.seq = builder.seq;
        this.createdAt = builder.createdAt;
        this.deleted = builder.deleted == null ? 0 : builder.deleted;
    }

    /**
     * 追加一条消息。
     *
     * @param sessionId 会话 ID
     * @param type      类型
     * @param content   文本
     * @param payload   完整结构
     * @param lastSeq   当前最大序号，可为 null
     * @return 新消息
     */
    public static ChatMessage append(
            Long sessionId,
            ChatMessageType type,
            String content,
            Map<String, Object> payload,
            Integer lastSeq) {
        int seq = lastSeq == null ? 0 : lastSeq + 1;
        return new Builder(sessionId, type)
                .content(content)
                .payload(payload)
                .seq(seq)
                .createdAt(LocalDateTime.now())
                .deleted(0)
                .build();
    }

    /**
     * 消息构建器。
     *
     * @author navms
     */
    public static final class Builder {

        private Long id;

        private final Long sessionId;

        private final ChatMessageType messageType;

        private String content;

        private Map<String, Object> payload;

        private Integer seq;

        private LocalDateTime createdAt;

        private Integer deleted;

        /**
         * @param sessionId   会话 ID，必填
         * @param messageType 类型，必填
         */
        public Builder(Long sessionId, ChatMessageType messageType) {
            if (sessionId == null) {
                throw new IllegalArgumentException("sessionId cannot be null");
            }
            if (messageType == null) {
                throw new IllegalArgumentException("messageType cannot be null");
            }
            this.sessionId = sessionId;
            this.messageType = messageType;
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
         * @param content 文本
         * @return this
         */
        public Builder content(String content) {
            this.content = content;
            return this;
        }

        /**
         * @param payload 完整结构
         * @return this
         */
        public Builder payload(Map<String, Object> payload) {
            this.payload = payload;
            return this;
        }

        /**
         * @param seq 序号
         * @return this
         */
        public Builder seq(Integer seq) {
            this.seq = seq;
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
         * @param deleted 逻辑删除标记
         * @return this
         */
        public Builder deleted(Integer deleted) {
            this.deleted = deleted;
            return this;
        }

        /**
         * @return 消息
         */
        public ChatMessage build() {
            return new ChatMessage(this);
        }
    }
}
