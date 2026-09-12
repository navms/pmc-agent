package io.github.navms.domain.chat.entity;

import io.github.navms.domain.chat.enums.ChatMessageType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 会话消息，正文为一条 AG-UI Message。
 *
 * @author navms
 */
@Getter
public class ChatMessage {

    private Long id;

    private final Long sessionId;

    private final ChatMessageType role;

    private final Map<String, Object> message;

    private final Integer seq;

    private final LocalDateTime createdAt;

    private final Integer deleted;

    private ChatMessage(Builder builder) {
        this.id = builder.id;
        this.sessionId = builder.sessionId;
        this.role = builder.role;
        this.message = builder.message == null ? Map.of() : new LinkedHashMap<>(builder.message);
        this.seq = builder.seq;
        this.createdAt = builder.createdAt;
        this.deleted = builder.deleted == null ? 0 : builder.deleted;
    }

    /**
     * 追加一条协议消息。
     *
     * @param sessionId 会话 ID
     * @param message   AG-UI Message
     * @param lastSeq   当前最大序号，可为 null
     * @return 新消息
     */
    public static ChatMessage append(Long sessionId, Map<String, Object> message, Integer lastSeq) {
        ChatMessageType role = ChatMessageType.fromCode(String.valueOf(message.get("role")));
        int seq = lastSeq == null ? 0 : lastSeq + 1;
        return new Builder(sessionId, role)
                .message(message)
                .seq(seq)
                .createdAt(LocalDateTime.now())
                .deleted(0)
                .build();
    }

    /**
     * 带 seq / createdAt 的协议对象，供历史 API 原样返回。
     *
     * @return AG-UI Message
     */
    public Map<String, Object> toProtocolMessage() {
        Map<String, Object> copy = new LinkedHashMap<>(message);
        Map<String, Object> metadata = new LinkedHashMap<>();
        Object existing = copy.get("metadata");
        if (existing instanceof Map<?, ?> map) {
            map.forEach((key, value) -> metadata.put(String.valueOf(key), value));
        }
        metadata.put("seq", seq);
        if (createdAt != null) {
            metadata.put("createdAt", createdAt.toString());
        }
        copy.put("metadata", metadata);
        return copy;
    }

    /**
     * 消息构建器。
     *
     * @author navms
     */
    public static final class Builder {

        private Long id;

        private final Long sessionId;

        private final ChatMessageType role;

        private Map<String, Object> message;

        private Integer seq;

        private LocalDateTime createdAt;

        private Integer deleted;

        /**
         * @param sessionId 会话 ID
         * @param role      AG-UI role
         */
        public Builder(Long sessionId, ChatMessageType role) {
            if (sessionId == null) {
                throw new IllegalArgumentException("sessionId cannot be null");
            }
            if (role == null) {
                throw new IllegalArgumentException("role cannot be null");
            }
            this.sessionId = sessionId;
            this.role = role;
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
         * @param message 协议对象
         * @return this
         */
        public Builder message(Map<String, Object> message) {
            this.message = message;
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
