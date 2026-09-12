package io.github.navms.web.agent.vo.message;

import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * 会话消息，按 messageType 多态序列化。
 * <p>
 * SSE/Jackson 多态载荷保持 class，不改为 record，以免破坏现有子类型序列化契约。
 *
 * @author navms
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "messageType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AssistantMessageResponse.class, name = "assistant"),
        @JsonSubTypes.Type(value = UserMessageResponse.class, name = "user"),
        @JsonSubTypes.Type(value = ToolResponseMessageResponse.class, name = "tool"),
        @JsonSubTypes.Type(value = ToolRequestMessageResponse.class, name = "tool-request"),
        @JsonSubTypes.Type(value = ToolRequestConfirmMessageResponse.class, name = "tool-confirm")
})
public interface MessageResponse {

    /**
     * 消息类型。
     */
    String getMessageType();

    /**
     * 消息内容。
     */
    String getContent();

    /**
     * 消息工厂类。
     */
    class MessageFactory {

        public static MessageResponse fromMessage(Message message) {
            switch (message) {
                case null -> {
                    return null;
                }
                case AssistantMessage assistantMessage -> {
                    if (assistantMessage.hasToolCalls()) {
                        return new ToolRequestMessageResponse(assistantMessage);
                    }
                    return new AssistantMessageResponse(assistantMessage);
                }
                case UserMessage userMessage -> {
                    return new UserMessageResponse(userMessage);
                }
                case ToolResponseMessage toolResponseMessage -> {
                    return new ToolResponseMessageResponse(toolResponseMessage);
                }
                default -> {
                }
            }
            throw new IllegalArgumentException("Unsupported message type: " + message.getClass().getName());
        }

        public static ToolRequestConfirmMessageResponse fromInterruptionMetadata(InterruptionMetadata interruptionMetadata) {
            if (interruptionMetadata == null) {
                return null;
            }
            return new ToolRequestConfirmMessageResponse(interruptionMetadata);
        }

    }
}
