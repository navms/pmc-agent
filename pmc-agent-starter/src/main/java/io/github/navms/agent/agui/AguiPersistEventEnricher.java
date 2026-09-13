package io.github.navms.agent.agui;

import io.agentscope.core.agui.adapter.strategy.AguiEventEnricher;
import io.agentscope.core.agui.adapter.strategy.AguiStreamContext;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.AguiMessage;
import io.agentscope.core.agui.model.RunAgentInput;
import io.agentscope.core.event.AgentEvent;
import io.github.navms.application.chat.service.ChatSessionAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将 AG-UI 事件聚合成协议 Message 落库。每个 run 独立缓冲。
 *
 * @author navms
 */
@Slf4j
@Component
public class AguiPersistEventEnricher implements AguiEventEnricher {

    private final ChatSessionAppService chatSessionAppService;

    private final Map<String, RunBuffer> runs = new ConcurrentHashMap<>();

    /**
     * @param chatSessionAppService 会话
     */
    public AguiPersistEventEnricher(ChatSessionAppService chatSessionAppService) {
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
        switch (event) {
            case AguiEvent.RunStarted started -> persistUserIfNeeded(
                    started.input() != null ? started.input() : context.getRunInput(), buffer);

            case AguiEvent.TextMessageStart start -> {
                buffer.text.putIfAbsent(start.messageId(), new StringBuilder());
                buffer.textAgent.putIfAbsent(start.messageId(), AguiAgentNames.GENERAL_CHAT);
            }

            case AguiEvent.TextMessageContent content -> {
                if (StringUtils.hasText(content.delta())) {
                    buffer.text.computeIfAbsent(content.messageId(), ignored -> new StringBuilder())
                            .append(content.delta());
                }
            }

            case AguiEvent.TextMessageEnd end -> flushText(buffer, end.messageId());

            case AguiEvent.ReasoningMessageStart start ->
                    buffer.reasoning.putIfAbsent(start.messageId(), new StringBuilder());

            case AguiEvent.ReasoningMessageContent content -> {
                if (StringUtils.hasText(content.delta())) {
                    buffer.reasoning.computeIfAbsent(content.messageId(), ignored -> new StringBuilder())
                            .append(content.delta());
                }
            }

            case AguiEvent.ReasoningMessageEnd end ->
                    flushReasoning(buffer, end.messageId(), AguiAgentNames.GENERAL_CHAT);

            case AguiEvent.ToolCallStart start -> {
                if (AguiAgentNames.hideParentTool(AguiAgentNames.GENERAL_CHAT, start.toolCallName())) {
                    buffer.hiddenTools.add(start.toolCallId());
                } else {
                    buffer.toolNames.put(start.toolCallId(), start.toolCallName());
                    buffer.toolArgs.putIfAbsent(start.toolCallId(), new StringBuilder());
                }
            }

            case AguiEvent.ToolCallArgs args -> {
                if (buffer.hiddenTools.contains(args.toolCallId())) {
                    break;
                }
                if (StringUtils.hasText(args.delta())) {
                    buffer.toolArgs.computeIfAbsent(args.toolCallId(), ignored -> new StringBuilder())
                            .append(args.delta());
                }
            }

            case AguiEvent.ToolCallEnd end -> {
                if (buffer.hiddenTools.remove(end.toolCallId())) {
                    buffer.toolArgs.remove(end.toolCallId());
                    buffer.toolNames.remove(end.toolCallId());
                } else {
                    persistToolCall(buffer, end.toolCallId(), AguiAgentNames.GENERAL_CHAT);
                }
            }

            case AguiEvent.ToolCallResult result -> {
                if (buffer.hiddenTools.contains(result.toolCallId())) {
                    break;
                }
                persistToolResult(
                        buffer,
                        result.toolCallId(),
                        buffer.toolNames.getOrDefault(result.toolCallId(), ""),
                        result.content() == null ? "" : result.content(),
                        AguiAgentNames.GENERAL_CHAT);
            }

            case AguiEvent.Custom custom -> applyCustom(custom, buffer);

            case AguiEvent.Raw raw -> applyRaw(raw, buffer);

            case AguiEvent.RunFinished finished -> {
                flushAllText(buffer);
                if (finished.outcome() instanceof AguiEvent.RunFinishedInterruptOutcome(
                        List<AguiEvent.Interrupt> interrupts
                )) {
                    persistInterruptRows(buffer, interruptRows(interrupts));
                    chatSessionAppService.markStatus(buffer.sessionId, "interrupted");
                } else if (!buffer.interruptPersisted) {
                    chatSessionAppService.markStatus(buffer.sessionId, "active");
                }
                runs.remove(buffer.key);
            }

            case AguiEvent.RunError ignored -> {
                flushAllText(buffer);
                chatSessionAppService.markStatus(buffer.sessionId, "active");
                runs.remove(buffer.key);
            }

            default -> log.debug("No handle AG-UI event {}", event);
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
            persistInterruptRows(buffer, interruptRowsFromCustom(value));
            chatSessionAppService.markStatus(buffer.sessionId, "interrupted");
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

    private void persistInterruptRows(RunBuffer buffer, List<Map<String, Object>> rows) {
        if (buffer.interruptPersisted || rows == null || rows.isEmpty()) {
            return;
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
        buffer.interruptPersisted = true;
    }

    private static List<Map<String, Object>> interruptRows(List<AguiEvent.Interrupt> interrupts) {
        if (interrupts == null || interrupts.isEmpty()) {
            return List.of();
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
        return rows;
    }

    private static List<Map<String, Object>> interruptRowsFromCustom(Map<String, Object> value) {
        List<Map<String, Object>> fromList = interruptMaps(value.get("interrupts"));
        if (!fromList.isEmpty()) {
            return fromList;
        }
        List<Map<String, Object>> fromCalls = interruptMaps(value.get("toolCalls"));
        if (!fromCalls.isEmpty()) {
            return fromCalls;
        }
        String toolCallId = stringVal(value.get("toolCallId"));
        String toolName = firstNonBlank(stringVal(value.get("toolName")), stringVal(value.get("name")));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", StringUtils.hasText(toolCallId) ? toolCallId : UUID.randomUUID().toString());
        row.put("reason", "permission_ask");
        row.put("toolCallId", toolCallId);
        row.put("message", StringUtils.hasText(toolName) ? "请求执行 " + toolName : "请求执行写操作");
        Map<String, Object> meta = new LinkedHashMap<>();
        if (StringUtils.hasText(toolName)) {
            meta.put("toolName", toolName);
        }
        row.put("metadata", meta);
        return List.of(row);
    }

    private static List<Map<String, Object>> interruptMaps(Object raw) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> source = asMap(item);
            if (source.isEmpty()) {
                continue;
            }
            String toolCallId = firstNonBlank(
                    stringVal(source.get("id")),
                    stringVal(source.get("toolCallId")),
                    stringVal(source.get("tool_call_id")));
            String toolName = firstNonBlank(
                    stringVal(source.get("toolName")),
                    stringVal(source.get("name")),
                    nestedToolName(source.get("metadata")));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", StringUtils.hasText(toolCallId) ? toolCallId : UUID.randomUUID().toString());
            row.put("reason", firstNonBlank(stringVal(source.get("reason")), "permission_ask"));
            row.put("toolCallId", toolCallId);
            row.put("message", firstNonBlank(
                    stringVal(source.get("message")),
                    StringUtils.hasText(toolName) ? "请求执行 " + toolName : "请求执行写操作"));
            Object metadata = source.get("metadata");
            if (metadata instanceof Map<?, ?>) {
                row.put("metadata", asMap(metadata));
            } else if (StringUtils.hasText(toolName)) {
                row.put("metadata", Map.of("toolName", toolName));
            } else {
                row.put("metadata", Map.of());
            }
            rows.add(row);
        }
        return rows;
    }

    private static String nestedToolName(Object metadata) {
        return stringVal(asMap(metadata).get("toolName"));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
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
        private boolean interruptPersisted;
        private Map<String, Object> pendingUsage;
        private final Map<String, StringBuilder> text = new LinkedHashMap<>();
        private final Map<String, String> textAgent = new LinkedHashMap<>();
        private final Map<String, StringBuilder> reasoning = new LinkedHashMap<>();
        private final Map<String, StringBuilder> subText = new LinkedHashMap<>();
        private final Map<String, StringBuilder> subReasoning = new LinkedHashMap<>();
        private final Map<String, StringBuilder> toolArgs = new LinkedHashMap<>();
        private final Map<String, StringBuilder> toolResults = new LinkedHashMap<>();
        private final Map<String, String> toolNames = new LinkedHashMap<>();
        private final Set<String> hiddenTools = new HashSet<>();

        private RunBuffer(Long sessionId, String key) {
            this.sessionId = sessionId;
            this.key = key;
        }
    }
}
