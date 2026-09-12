package io.github.navms.config.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.navms.web.agent.vo.message.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 替代 {@link AgentTool#getFunctionToolCallback(ReactAgent)}：
 * 同步执行子 Agent，采集内部 tool-request / tool 写入 {@link SubAgentTraceRegistry}，
 * 仍只把终答文本返回给 Supervisor（与框架 AgentTool 一致）。
 *
 * @author navms
 */
@Slf4j
public final class TracingAgentToolCallback implements ToolCallback {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ReactAgent agent;

    private final ToolCallback definitionDelegate;

    private TracingAgentToolCallback(ReactAgent agent, ToolCallback definitionDelegate) {
        this.agent = agent;
        this.definitionDelegate = definitionDelegate;
    }

    /**
     * @param agent 子 Agent
     * @return 带轨迹采集的 ToolCallback
     */
    public static ToolCallback from(ReactAgent agent) {
        return new TracingAgentToolCallback(agent, AgentTool.getFunctionToolCallback(agent));
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return definitionDelegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return definitionDelegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String input = extractInputValue(toolInput);
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasLength(agent.instruction())) {
            messages.add(AgentInstructionMessage.builder().text(agent.instruction()).build());
        }
        messages.add(new UserMessage(input));

        // 独立 threadId，避免与 Supervisor / 其他子 Agent checkpoint 冲突
        RunnableConfig config = RunnableConfig.builder()
                .threadId(agent.name() + "-" + UUID.randomUUID())
                .build();

        try {
            Optional<OverAllState> stateOpt = agent.invoke(messages, config);
            List<Message> resultMessages = stateOpt
                    .flatMap(state -> state.value("messages"))
                    .filter(List.class::isInstance)
                    .map(value -> {
                        @SuppressWarnings("unchecked")
                        List<Message> list = (List<Message>) value;
                        return list;
                    })
                    .orElseThrow(() -> new IllegalStateException(
                            "Failed to execute agent tool or failed to get agent tool result: " + agent.name()));

            List<MessageResponse> nested = new ArrayList<>();
            for (Message message : resultMessages) {
                if (message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
                    nested.add(MessageResponse.MessageFactory.fromMessage(assistantMessage));
                }
                else if (message instanceof ToolResponseMessage) {
                    nested.add(MessageResponse.MessageFactory.fromMessage(message));
                }
            }
            SubAgentTraceRegistry.push(agent.name(), nested);

            Message last = resultMessages.get(resultMessages.size() - 1);
            if (last instanceof AssistantMessage assistantMessage) {
                if (StringUtils.hasLength(assistantMessage.getText())) {
                    return assistantMessage.getText();
                }
                log.warn("Agent tool {} returned empty AssistantMessage text", agent.name());
                return "Done";
            }
            throw new IllegalStateException(
                    "Agent tool last message is not AssistantMessage: " + agent.name());
        }
        catch (RuntimeException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to execute tracing agent tool: " + agent.name(), ex);
        }
    }

    /**
     * 与框架 AgentTool 一致：从 {"input":"..."} 中取出实际指令。
     */
    private static String extractInputValue(String toolInput) {
        if (!StringUtils.hasText(toolInput)) {
            return toolInput;
        }
        try {
            ObjectMapper mapper = JsonParser.getObjectMapper();
            Map<String, Object> map = mapper.readValue(toolInput, MAP_TYPE);
            if (map != null && map.containsKey("input") && map.get("input") != null) {
                Object value = map.get("input");
                if (value instanceof String text) {
                    return text;
                }
                return mapper.writeValueAsString(value);
            }
        }
        catch (Exception ignored) {
            // 非 JSON 时原样返回
        }
        return toolInput;
    }
}
