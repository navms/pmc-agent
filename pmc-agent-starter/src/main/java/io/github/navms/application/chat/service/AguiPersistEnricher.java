package io.github.navms.application.chat.service;

import io.agentscope.core.agui.adapter.strategy.AguiEventEnricher;
import io.agentscope.core.agui.adapter.strategy.AguiStreamContext;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.AguiMessage;
import io.agentscope.core.agui.model.RunAgentInput;
import io.agentscope.core.event.AgentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将 AG-UI 事件聚合成协议 Message 落库。每个 run 独立缓冲。
 *
 * @author navms
 */
@Slf4j
public class AguiPersistEnricher implements AguiEventEnricher {

    private final ChatSessionAppService chatSessionAppService;

    private final Map<String, RunBuffer> runs = new ConcurrentHashMap<>();

    /**
     * @param chatSessionAppService 会话
     */
    public AguiPersistEnricher(ChatSessionAppService chatSessionAppService) {
        this.chatSessionAppService = chatSessionAppService;
    }

    @Override
    public List<AguiEvent> enrich(AgentEvent source, List<AguiEvent> events, AguiStreamContext context) {
        if (events == null || events.isEmpty() || context == null) {
            return events == null ? List.of() : events;
        }
        Long sessionId = parseSessionId(context.getThreadId());
        if (sessionId == null) {
            return events;
        }
        String key = context.getThreadId() + ":" + context.getRunId();
        RunBuffer buffer = runs.computeIfAbsent(key, ignored -> new RunBuffer(sessionId, key));
        for (AguiEvent event : events) {
            try {
                apply(event, buffer, context);
            } catch (Exception e) {
                log.warn("Failed to persist AG-UI event {}", event.getType(), e);
            }
        }
        return events;
    }

    private void apply(AguiEvent event, RunBuffer buffer, AguiStreamContext context) {
        if (event instanceof AguiEvent.RunStarted started) {
            persistUserIfNeeded(started.input() != null ? started.input() : context.getRunInput(), buffer);
            return;
        }
        if (event instanceof AguiEvent.TextMessageStart start) {
            buffer.text.putIfAbsent(start.messageId(), new StringBuilder());
            buffer.textAgent.putIfAbsent(start.messageId(), AguiAgentNames.GENERAL_CHAT);
            return;
        }
        if (event instanceof AguiEvent.TextMessageContent content) {
            if (StringUtils.hasText(content.delta())) {
                buffer.text.computeIfAbsent(content.messageId(), ignored -> new StringBuilder()).append(content.delta());
            }
            return;
        }
        if (event instanceof AguiEvent.TextMessageEnd end) {
            flushText(buffer, end.messageId());
            return;
        }
        if (event instanceof AguiEvent.ReasoningMessageStart start) {
            buffer.reasoning.putIfAbsent(start.messageId(), new StringBuilder());
            return;
        }
        if (event instanceof AguiEvent.ReasoningMessageContent content) {
            if (StringUtils.hasText(content.delta())) {
                buffer.reasoning.computeIfAbsent(content.messageId(), ignored -> new StringBuilder()).append(content.delta());
            }
            return;
        }
        if (event instanceof AguiEvent.ReasoningMessageEnd end) {
            flushReasoning(buffer, end.messageId(), AguiAgentNames.GENERAL_CHAT);
            return;
        }
        if (event instanceof AguiEvent.ToolCallStart start) {
            if (AguiAgentNames.hideParentTool(AguiAgentNames.GENERAL_CHAT, start.toolCallName())) {
                buffer.hiddenTools.add(start.toolCallId());
                return;
            }
            buffer.toolNames.put(start.toolCallId(), start.toolCallName());
            buffer.toolArgs.putIfAbsent(start.toolCallId(), new StringBuilder());
            return;
        }
        if (event instanceof AguiEvent.ToolCallArgs args) {
            if (buffer.hiddenTools.contains(args.toolCallId())) {
                return;
            }
            if (StringUtils.hasText(args.delta())) {
                buffer.toolArgs.computeIfAbsent(args.toolCallId(), ignored -> new StringBuilder()).append(args.delta());
            }
            return;
        }
        if (event instanceof AguiEvent.ToolCallEnd end) {
            if (buffer.hiddenTools.remove(end.toolCallId())) {
                buffer.toolArgs.remove(end.toolCallId());
                buffer.toolNames.remove(end.toolCallId());
                return;
            }
            persistToolCall(buffer, end.toolCallId(), AguiAgentNames.GENERAL_CHAT);
            return;
        }
        if (event instanceof AguiEvent.ToolCallResult result) {
            if (buffer.hiddenTools.contains(result.toolCallId())) {
                return;
            }
            persistToolResult(
                    buffer,
                    result.toolCallId(),
                    buffer.toolNames.getOrDefault(result.toolCallId(), ""),
                    result.content() == null ? "" : result.content(),
                    AguiAgentNames.GENERAL_CHAT);
            return;
        }
        if (event instanceof AguiEvent.Custom custom) {
            applyCustom(custom, buffer);
            return;
        }
        if (event instanceof AguiEvent.Raw raw) {
            applyRaw(raw, buffer);
            return;
        }
        if (event instanceof AguiEvent.RunFinished finished) {
            flushAllText(buffer);
            if (finished.outcome() instanceof AguiEvent.RunFinishedInterruptOutcome interruptOutcome) {
                persistInterrupts(buffer, interruptOutcome.interrupts());
                chatSessionAppService.markStatus(buffer.sessionId, "interrupted");
            } else {
                chatSessionAppService.markStatus(buffer.sessionId, "active");
            }
            runs.remove(buffer.key);
            return;
        }
        if (event instanceof AguiEvent.RunError) {
            flushAllText(buffer);
            chatSessionAppService.markStatus(buffer.sessionId, "active");
            runs.remove(buffer.key);
        }
    }

    private void applyCustom(AguiEvent.Custom custom, RunBuffer buffer) {
        Map<String, Object> value = asMap(custom.value());
        String agentName = AguiAgentNames.fromSource(stringVal(value.get("source")));
        String type = stringVal(value.get("type"));
        if ("token_usage".equals(custom.name())) {
            Map<String, Object> usage = tokenUsage(value);
            if (usage != null) {
                buffer.pendingUsage = usage;
            }
            return;
        }
        if ("subagent.text".equals(custom.name())) {
            String delta = stringVal(value.get("delta"));
            if (StringUtils.hasText(delta)) {
                buffer.subText.computeIfAbsent(agentName, ignored -> new StringBuilder()).append(delta);
            }
            return;
        }
        if ("subagent.thinking".equals(custom.name())) {
            String delta = stringVal(value.get("delta"));
            if (StringUtils.hasText(delta)) {
                buffer.subReasoning.computeIfAbsent(agentName, ignored -> new StringBuilder()).append(delta);
            }
            return;
        }
        if ("subagent.tool_call".equals(custom.name())) {
            flushSubReasoning(buffer, agentName);
            flushSubText(buffer, agentName);
            String toolCallId = stringVal(value.get("toolCallId"));
            String toolName = stringVal(value.get("toolName"));
            if ("TOOL_CALL_END".equals(type) && StringUtils.hasText(toolCallId)) {
                buffer.toolNames.put(toolCallId, toolName);
                persistToolCall(buffer, toolCallId, agentName);
            } else if ("TOOL_CALL_START".equals(type) && StringUtils.hasText(toolCallId)) {
                buffer.toolNames.put(toolCallId, toolName);
                buffer.toolArgs.putIfAbsent(toolCallId, new StringBuilder());
            }
            return;
        }
        if ("subagent.tool_result".equals(custom.name())) {
            String toolCallId = stringVal(value.get("toolCallId"));
            String toolName = stringVal(value.get("toolName"));
            String data = takeBuf(buffer.toolResults, toolCallId);
            persistToolResult(buffer, toolCallId, toolName, data, agentName);
            return;
        }
        if ("subagent.require_confirm".equals(custom.name())) {
            flushSubReasoning(buffer, agentName);
            flushSubText(buffer, agentName);
            return;
        }
        if ("subagent.lifecycle".equals(custom.name()) && "AGENT_END".equals(type)) {
            flushSubReasoning(buffer, agentName);
            flushSubText(buffer, agentName);
        }
    }

    private void applyRaw(AguiEvent.Raw raw, RunBuffer buffer) {
        Map<String, Object> event = asMap(raw.event());
        String type = stringVal(event.get("type"));
        String agentName = AguiAgentNames.fromSource(raw.source());
        String toolCallId = stringVal(event.get("toolCallId"));
        String delta = stringVal(event.get("delta"));
        if ("TOOL_CALL_DELTA".equals(type) && StringUtils.hasText(toolCallId) && StringUtils.hasText(delta)) {
            if (AguiAgentNames.hideParentTool(agentName, stringVal(event.get("toolCallName")))) {
                return;
            }
            buffer.toolArgs.computeIfAbsent(toolCallId, ignored -> new StringBuilder()).append(delta);
            return;
        }
        if ("TOOL_RESULT_TEXT_DELTA".equals(type) && StringUtils.hasText(toolCallId) && StringUtils.hasText(delta)) {
            buffer.toolResults.computeIfAbsent(toolCallId, ignored -> new StringBuilder()).append(delta);
        }
    }

    private void persistUserIfNeeded(RunAgentInput input, RunBuffer buffer) {
        if (buffer.userPersisted || input == null) {
            return;
        }
        if (input.hasResume()) {
            return;
        }
        AguiMessage lastUser = null;
        for (Object item : input.getMessages()) {
            if (item instanceof AguiMessage message && message.isUserMessage()) {
                lastUser = message;
            }
        }
        if (lastUser == null) {
            return;
        }
        String text = lastUser.getTextContent();
        if (!StringUtils.hasText(text)) {
            return;
        }
        chatSessionAppService.appendUserPrompt(buffer.sessionId, text);
        buffer.userPersisted = true;
    }

    private void persistInterrupts(RunBuffer buffer, List<AguiEvent.Interrupt> interrupts) {
        if (interrupts == null || interrupts.isEmpty()) {
            return;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AguiEvent.Interrupt interrupt : interrupts) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", interrupt.id());
            row.put("reason", interrupt.reason());
            row.put("toolCallId", interrupt.toolCallId());
            row.put("message", interrupt.message());
            row.put("metadata", interrupt.metadata());
            rows.add(row);
        }
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("interrupts", rows);
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("id", UUID.randomUUID().toString());
        message.put("role", "activity");
        message.put("activityType", "TOOL_CONFIRM");
        message.put("content", content);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("agentName", AguiAgentNames.GENERAL_CHAT);
        message.put("metadata", metadata);
        chatSessionAppService.appendMessage(buffer.sessionId, message);
    }

    private void flushSubText(RunBuffer buffer, String agentName) {
        StringBuilder text = buffer.subText.remove(agentName);
        if (text == null || text.isEmpty()) {
            return;
        }
        persistAssistant(buffer, UUID.randomUUID().toString(), text.toString(), agentName);
    }

    private void flushText(RunBuffer buffer, String messageId) {
        StringBuilder text = buffer.text.remove(messageId);
        String agentName = buffer.textAgent.remove(messageId);
        if (text == null || text.isEmpty()) {
            return;
        }
        persistAssistant(
                buffer,
                messageId,
                text.toString(),
                agentName == null ? AguiAgentNames.GENERAL_CHAT : agentName);
    }

    private void flushAllText(RunBuffer buffer) {
        for (Map.Entry<String, StringBuilder> entry : List.copyOf(buffer.reasoning.entrySet())) {
            flushReasoning(buffer, entry.getKey(), AguiAgentNames.GENERAL_CHAT);
        }
        for (String agentName : List.copyOf(buffer.subReasoning.keySet())) {
            flushSubReasoning(buffer, agentName);
        }
        for (String messageId : List.copyOf(buffer.text.keySet())) {
            flushText(buffer, messageId);
        }
        for (String agentName : List.copyOf(buffer.subText.keySet())) {
            flushSubText(buffer, agentName);
        }
    }

    private void flushReasoning(RunBuffer buffer, String messageId, String agentName) {
        StringBuilder text = buffer.reasoning.remove(messageId);
        if (text == null || text.isEmpty()) {
            return;
        }
        persistReasoning(buffer, messageId, text.toString(), agentName);
    }

    private void flushSubReasoning(RunBuffer buffer, String agentName) {
        StringBuilder text = buffer.subReasoning.remove(agentName);
        if (text == null || text.isEmpty()) {
            return;
        }
        persistReasoning(buffer, UUID.randomUUID().toString(), text.toString(), agentName);
    }

    private void persistAssistant(RunBuffer buffer, String id, String text, String agentName) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("id", id);
        message.put("role", "assistant");
        message.put("content", text);
        message.put("name", agentName);
        if (buffer.pendingUsage != null) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("tokenUsage", buffer.pendingUsage);
            message.put("metadata", metadata);
            buffer.pendingUsage = null;
        }
        chatSessionAppService.appendMessage(buffer.sessionId, message);
    }

    private void persistReasoning(RunBuffer buffer, String id, String text, String agentName) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("agentName", agentName);
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("id", id);
        message.put("role", "reasoning");
        message.put("content", text);
        message.put("metadata", metadata);
        chatSessionAppService.appendMessage(buffer.sessionId, message);
    }

    private void persistToolCall(RunBuffer buffer, String toolCallId, String agentName) {
        String name = buffer.toolNames.getOrDefault(toolCallId, "");
        String args = takeBuf(buffer.toolArgs, toolCallId);
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("arguments", args);
        Map<String, Object> call = new LinkedHashMap<>();
        call.put("id", toolCallId);
        call.put("type", "function");
        call.put("function", function);
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("id", UUID.randomUUID().toString());
        message.put("role", "assistant");
        message.put("name", agentName);
        message.put("toolCalls", List.of(call));
        chatSessionAppService.appendMessage(buffer.sessionId, message);
    }

    private void persistToolResult(RunBuffer buffer, String toolCallId, String toolName, String data, String agentName) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("toolName", toolName);
        metadata.put("agentName", agentName);
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("id", UUID.randomUUID().toString());
        message.put("role", "tool");
        message.put("toolCallId", toolCallId);
        message.put("content", data == null ? "" : data);
        message.put("metadata", metadata);
        chatSessionAppService.appendMessage(buffer.sessionId, message);
    }

    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> copy.put(String.valueOf(k), v));
            return copy;
        }
        return Map.of();
    }

    private static Map<String, Object> tokenUsage(Map<String, Object> value) {
        Object delta = value.get("delta");
        Map<String, Object> source = delta instanceof Map<?, ?> map ? asMap(map) : value;
        Object input = first(source, "inputTokens", "promptTokens");
        Object output = first(source, "outputTokens", "completionTokens");
        Object total = first(source, "totalTokens");
        if (input == null && output == null && total == null) {
            return null;
        }
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("promptTokens", toInt(input));
        usage.put("completionTokens", toInt(output));
        usage.put("totalTokens", toInt(total));
        return usage;
    }

    private static Object first(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            if (source.containsKey(key)) {
                return source.get(key);
            }
        }
        return null;
    }

    private static Integer toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static String stringVal(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String takeBuf(Map<String, StringBuilder> buf, String key) {
        StringBuilder builder = buf.remove(key);
        return builder == null ? "" : builder.toString();
    }

    private static Long parseSessionId(String threadId) {
        if (!StringUtils.hasText(threadId)) {
            return null;
        }
        try {
            return Long.valueOf(threadId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final class RunBuffer {
        private final Long sessionId;
        private final String key;
        private boolean userPersisted;
        private Map<String, Object> pendingUsage;
        private final Map<String, StringBuilder> text = new LinkedHashMap<>();
        private final Map<String, String> textAgent = new LinkedHashMap<>();
        private final Map<String, StringBuilder> reasoning = new LinkedHashMap<>();
        private final Map<String, StringBuilder> subText = new LinkedHashMap<>();
        private final Map<String, StringBuilder> subReasoning = new LinkedHashMap<>();
        private final Map<String, StringBuilder> toolArgs = new LinkedHashMap<>();
        private final Map<String, StringBuilder> toolResults = new LinkedHashMap<>();
        private final Map<String, String> toolNames = new LinkedHashMap<>();
        private final java.util.Set<String> hiddenTools = new java.util.HashSet<>();

        private RunBuffer(Long sessionId, String key) {
            this.sessionId = sessionId;
            this.key = key;
        }
    }
}
