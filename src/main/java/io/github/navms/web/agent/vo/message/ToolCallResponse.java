package io.github.navms.web.agent.vo.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;

/**
 * 工具调用。
 *
 * @author navms
 */
@Data
public class ToolCallResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("arguments")
    private String arguments;

    public ToolCallResponse(AssistantMessage.ToolCall toolCall) {
        this.id = toolCall.id();
        this.type = toolCall.type();
        this.name = toolCall.name();
        this.arguments = toolCall.arguments();
    }

}
