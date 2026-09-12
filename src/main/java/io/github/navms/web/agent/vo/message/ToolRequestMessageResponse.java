package io.github.navms.web.agent.vo.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 助手发起的工具请求消息。
 *
 * @author navms
 */
@Data
public class ToolRequestMessageResponse implements MessageResponse {

    @JsonProperty("messageType")
    private String messageType = "tool-request";

    @JsonProperty("content")
    private String content;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("toolCalls")
    private List<ToolCallResponse> toolCalls = new ArrayList<>();

    public ToolRequestMessageResponse(AssistantMessage message) {
        this.content = message.getText();
        this.metadata = new HashMap<>(message.getMetadata());
        for (AssistantMessage.ToolCall toolCall : message.getToolCalls()) {
            this.toolCalls.add(new ToolCallResponse(toolCall));
        }
    }

}
