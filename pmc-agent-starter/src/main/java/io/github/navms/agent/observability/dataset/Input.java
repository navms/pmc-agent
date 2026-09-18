package io.github.navms.agent.observability.dataset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.agentscope.core.util.JsonUtils;
import io.github.navms.domain.chat.entity.ChatMessage;
import io.github.navms.domain.chat.entity.ChatSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Langfuse base-case Dataset item 的 input。
 *
 * @param title     会话标题
 * @param sessionId 聊天会话 id
 * @param messages  AG-UI 协议消息
 * @author navms
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Input(String title, Long sessionId, List<Message> messages) {

    /**
     * 从会话快照构造，字段与写入 Langfuse 的 JSON 一致。
     *
     * @param session 会话
     * @param rows    消息行
     * @return input
     */
    public static Input from(ChatSession session, List<ChatMessage> rows) {
        if (session == null) {
            return new Input(null, null, List.of());
        }
        String title = session.getTitle() == null ? null : session.getTitle().value();
        List<Message> messages = new ArrayList<>();
        if (rows != null) {
            for (ChatMessage row : rows) {
                if (row == null) {
                    continue;
                }
                Message message = JsonUtils.getJsonCodec().convertValue(row.toProtocolMessage(), Message.class);
                if (message != null) {
                    messages.add(message);
                }
            }
        }
        return new Input(title, session.getId(), List.copyOf(messages));
    }

    /**
     * @return 可重放的用户提问
     */
    public List<Message> userTurns() {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<Message> turns = new ArrayList<>();
        for (Message message : messages) {
            if (message != null && message.userTurn()) {
                turns.add(message);
            }
        }
        return List.copyOf(turns);
    }

    /**
     * 一条 AG-UI Message。
     *
     * @param id         消息 id
     * @param role       user / assistant / reasoning / tool
     * @param content    正文
     * @param name       助手名
     * @param metadata   序号、agent、token 等
     * @param toolCalls  工具调用
     * @param toolCallId tool 结果对应的 call id
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            String id,
            String role,
            String content,
            String name,
            MessageMetadata metadata,
            List<ToolCall> toolCalls,
            String toolCallId) {

        /**
         * @return 是否为可重放的用户提问
         */
        public boolean userTurn() {
            return "user".equalsIgnoreCase(role) && content != null && !content.isBlank();
        }
    }

    /**
     * 消息 metadata。
     *
     * @param seq        会话内序号
     * @param createdAt  创建时间原文
     * @param agentName  产出该消息的 Agent
     * @param toolName   tool 结果对应的工具名
     * @param tokenUsage token 用量
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MessageMetadata(
            Integer seq,
            String createdAt,
            String agentName,
            String toolName,
            TokenUsage tokenUsage) {
    }

    /**
     * token 用量。
     *
     * @param totalTokens      总 token
     * @param promptTokens     输入 token
     * @param completionTokens 输出 token
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenUsage(Integer totalTokens, Integer promptTokens, Integer completionTokens) {
    }

    /**
     * assistant 上的一次工具调用。
     *
     * @param id       call id
     * @param type     一般为 function
     * @param function 函数名与参数
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolCall(String id, String type, Function function) {
    }

    /**
     * 工具函数。
     *
     * @param name      工具名
     * @param arguments 参数 JSON 字符串（协议里可能为空串）
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Function(String name, String arguments) {
    }

}
