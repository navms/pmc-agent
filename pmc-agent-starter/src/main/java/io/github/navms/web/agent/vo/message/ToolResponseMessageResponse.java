package io.github.navms.web.agent.vo.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具执行结果消息。
 *
 * @author navms
 */
@Data
public class ToolResponseMessageResponse implements MessageResponse {

    @JsonProperty("messageType")
    private String messageType = "tool";

    @JsonProperty("content")
    private String content;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("responses")
    private List<ToolResponse> responses = new ArrayList<>();

    public ToolResponseMessageResponse(ToolResponseMessage message) {
        this.content = message.getText();
        this.metadata = new HashMap<>(message.getMetadata());
        for (ToolResponseMessage.ToolResponse response : message.getResponses()) {
            this.responses.add(new ToolResponse(response));
        }
    }

    /**
     * 单条工具响应。
     *
     * @author navms
     */
    @Data
    public static class ToolResponse {

        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("responseData")
        private String responseData;

        public ToolResponse(ToolResponseMessage.ToolResponse response) {
            this.id = response.id();
            this.name = response.name();
            this.responseData = response.responseData();
        }

    }
}
