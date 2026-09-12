package io.github.navms.web.agent.vo.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 助手消息。
 *
 * @author navms
 */
@Data
public class AssistantMessageResponse implements MessageResponse {

    @JsonProperty("messageType")
    private String messageType = "assistant";

    @JsonProperty("content")
    private String content;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("toolCalls")
    private List<ToolCallResponse> toolCalls = new ArrayList<>();

    public AssistantMessageResponse(AssistantMessage message) {
        this.content = message.getText();
        this.metadata = new HashMap<>(message.getMetadata());
        for (AssistantMessage.ToolCall toolCall : message.getToolCalls()) {
            this.toolCalls.add(new ToolCallResponse(toolCall));
        }
    }

    /**
     * 将 AgentTool 子 Agent 的自然语言提升为助手消息时使用。
     *
     * @param content 正文
     */
    public AssistantMessageResponse(String content) {
        this.content = content;
        this.metadata = new HashMap<>();
    }

}
