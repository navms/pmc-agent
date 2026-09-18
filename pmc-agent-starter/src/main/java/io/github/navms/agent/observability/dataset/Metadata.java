package io.github.navms.agent.observability.dataset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Langfuse base-case Dataset item 的 metadata。
 *
 * @param userId            点踩用户
 * @param dislikedMessageId 被踩的消息 id
 * @param rating            目前仅 down
 * @author navms
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Metadata(String userId, String dislikedMessageId, String rating) {

    /**
     * @param userId    用户
     * @param messageId 被踩消息
     * @return metadata
     */
    public static Metadata dislike(String userId, String messageId) {
        return new Metadata(userId, messageId, "down");
    }

}
