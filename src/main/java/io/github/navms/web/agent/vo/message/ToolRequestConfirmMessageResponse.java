package io.github.navms.web.agent.vo.message;

import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HITL 工具确认消息。
 *
 * @author navms
 */
@Data
public class ToolRequestConfirmMessageResponse implements MessageResponse {

    @JsonProperty("messageType")
    private String messageType = "tool-confirm";

    @JsonProperty("content")
    private String content = "";

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("toolFeedback")
    private List<ToolFeedbackResponse> toolFeedback = new ArrayList<>();

    @JsonProperty("toolsAutomaticallyApproved")
    private List<ToolCallResponse> toolsAutomaticallyApproved = new ArrayList<>();

    public ToolRequestConfirmMessageResponse(InterruptionMetadata interruptionMetadata) {
        this.metadata = new HashMap<>(interruptionMetadata.metadata().orElse(Map.of()));
        List<?> rawFeedbacks = interruptionMetadata.toolFeedbacks();
        if (rawFeedbacks != null) {
            for (Object item : rawFeedbacks) {
                if (item instanceof InterruptionMetadata.ToolFeedback feedback) {
                    this.toolFeedback.add(ToolFeedbackResponse.from(feedback));
                }
            }
        }
        List<AssistantMessage.ToolCall> approved = interruptionMetadata.getToolsAutomaticallyApproved();
        if (approved != null) {
            for (AssistantMessage.ToolCall toolCall : approved) {
                this.toolsAutomaticallyApproved.add(new ToolCallResponse(toolCall));
            }
        }
    }

    /**
     * 工具确认反馈。
     *
     * @author navms
     */
    @Data
    public static class ToolFeedbackResponse {

        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("arguments")
        private String arguments;

        @JsonProperty("result")
        private String result;

        @JsonProperty("description")
        private String description;

        public static ToolFeedbackResponse from(InterruptionMetadata.ToolFeedback feedback) {
            ToolFeedbackResponse response = new ToolFeedbackResponse();
            response.setId(feedback.getId());
            response.setName(feedback.getName());
            response.setArguments(feedback.getArguments());
            response.setDescription(feedback.getDescription());
            if (feedback.getResult() != null) {
                response.setResult(feedback.getResult().name());
            }
            return response;
        }
    }

}
