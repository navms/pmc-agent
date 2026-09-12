package io.github.navms.web.agent.vo.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户消息。
 *
 * @author navms
 */
@Data
public class UserMessageResponse implements MessageResponse {

    @JsonProperty("messageType")
    private String messageType = "user";

    @JsonProperty("content")
    private String content;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("media")
    private List<Object> media = new ArrayList<>();

    public UserMessageResponse(UserMessage message) {
        this.content = message.getText();
        this.metadata = new HashMap<>(message.getMetadata());
    }

}
