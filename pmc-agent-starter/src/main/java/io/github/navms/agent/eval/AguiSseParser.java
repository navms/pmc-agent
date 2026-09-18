package io.github.navms.agent.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 解析 AG-UI SSE，抽出子 Agent、写工具、HITL 与澄清信号。
 *
 * @author navms
 */
public final class AguiSseParser {

    private static final Set<String> SPAWN_NAME_KEYS = Set.of("name", "agent", "subagent", "agent_name", "agentName");

    private final ObjectMapper objectMapper;

    public AguiSseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @param reader SSE 流
     * @return 观察结果（不含耗时）
     */
    public TranscriptObservation parse(Reader reader) throws IOException {
        ParseState state = new ParseState();
        BufferedReader buffered = reader instanceof BufferedReader b ? b : new BufferedReader(reader);
        String event = null;
        StringBuilder data = new StringBuilder();
        String line;
        while ((line = buffered.readLine()) != null) {
            if (line.startsWith("event:")) {
                event = line.substring("event:".length()).trim();
            } else if (line.startsWith("data:")) {
                if (!data.isEmpty()) {
                    data.append('\n');
                }
                data.append(line.substring("data:".length()).trim());
            } else if (line.isBlank()) {
                if (!data.isEmpty()) {
                    applyEvent(state, event, data.toString());
                }
                event = null;
                data.setLength(0);
            }
        }
        if (!data.isEmpty()) {
            applyEvent(state, event, data.toString());
        }
        for (Map.Entry<String, StringBuilder> entry : state.toolArgs.entrySet()) {
            ingestSpawnArgs(state, state.toolNames.get(entry.getKey()), entry.getValue().toString());
        }
        return new TranscriptObservation(
                Set.copyOf(state.spawnedAgents),
                Set.copyOf(state.writeTools),
                state.interrupted,
                List.copyOf(state.interruptIds),
                state.clarified,
                state.assistantText.toString(),
                state.harnessError,
                0L);
    }

    private void applyEvent(ParseState state, String eventName, String raw) {
        JsonNode node;
        try {
            node = objectMapper.readTree(raw);
        } catch (Exception e) {
            return;
        }
        if (node == null || node.isNull() || node.isMissingNode()) {
            return;
        }
        String type = firstText(node, "type", eventName);
        if ("RUN_ERROR".equals(type) || "RUN_ERROR".equals(eventName)) {
            state.harnessError = firstText(node, "message", "AG-UI RUN_ERROR");
            return;
        }
        if ("RUN_FINISHED".equals(type) || "RUN_FINISHED".equals(eventName)) {
            applyFinished(state, node);
            return;
        }
        if ("CUSTOM".equals(type) || "CUSTOM".equals(eventName)) {
            applyCustom(state, node);
            return;
        }
        if ("TEXT_MESSAGE_CONTENT".equals(type) || "TEXT_MESSAGE_CONTENT".equals(eventName)) {
            String delta = firstText(node, "delta", "");
            if (StringUtils.hasText(delta)) {
                state.assistantText.append(delta);
            }
            return;
        }
        if ("TOOL_CALL_START".equals(type) || "TOOL_CALL_START".equals(eventName)) {
            String id = firstText(node, "toolCallId", "");
            String name = firstText(node, "toolCallName", firstText(node, "toolName", ""));
            if (StringUtils.hasText(id) && StringUtils.hasText(name)) {
                state.toolNames.put(id, name);
            }
            recordTool(state, name);
            return;
        }
        if ("TOOL_CALL_ARGS".equals(type) || "TOOL_CALL_DELTA".equals(type)
                || "TOOL_CALL_ARGS".equals(eventName) || "TOOL_CALL_DELTA".equals(eventName)) {
            String id = firstText(node, "toolCallId", "");
            String delta = firstText(node, "delta", "");
            if (StringUtils.hasText(id) && StringUtils.hasText(delta)) {
                state.toolArgs.computeIfAbsent(id, ignored -> new StringBuilder()).append(delta);
            }
            return;
        }
        if ("TOOL_CALL_END".equals(type) || "TOOL_CALL_END".equals(eventName)) {
            String id = firstText(node, "toolCallId", "");
            String name = state.toolNames.getOrDefault(id, firstText(node, "toolCallName", ""));
            String args = state.toolArgs.containsKey(id) ? state.toolArgs.remove(id).toString() : "";
            ingestSpawnArgs(state, name, args);
        }
    }

    private void applyFinished(ParseState state, JsonNode node) {
        String outcome = firstText(node, "outcome", "");
        if ("interrupt".equalsIgnoreCase(outcome)) {
            state.interrupted = true;
        }
        JsonNode interrupts = node.get("interrupts");
        if (interrupts != null && interrupts.isArray()) {
            for (JsonNode item : interrupts) {
                String id = firstText(item, "id", "");
                if (StringUtils.hasText(id)) {
                    state.interruptIds.add(id);
                    state.interrupted = true;
                }
            }
        }
    }

    private void applyCustom(ParseState state, JsonNode node) {
        String name = firstText(node, "name", "");
        JsonNode value = node.get("value");
        if ("intent_clarify.start".equals(name) || "intent_clarify.end".equals(name)) {
            state.clarified = true;
            return;
        }
        if ("subagent.lifecycle".equals(name)) {
            String source = value == null ? "" : firstText(value, "source", "");
            String agent = agentFromSource(source);
            if (EvalCatalog.isBusinessAgent(agent)) {
                state.spawnedAgents.add(agent);
            }
            return;
        }
        if ("subagent.tool_call".equals(name) && value != null) {
            String toolName = firstText(value, "toolName", "");
            recordTool(state, toolName);
        }
    }

    private void recordTool(ParseState state, String name) {
        if (!StringUtils.hasText(name)) {
            return;
        }
        if (EvalCatalog.isWriteTool(name)) {
            state.writeTools.add(name);
        }
        if (EvalCatalog.isBusinessAgent(name)) {
            state.spawnedAgents.add(name);
        }
    }

    private void ingestSpawnArgs(ParseState state, String toolName, String args) {
        if (!EvalCatalog.SPAWN_TOOL.equals(toolName) || !StringUtils.hasText(args)) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(args);
            String agent = spawnAgent(node);
            if (EvalCatalog.isBusinessAgent(agent)) {
                state.spawnedAgents.add(agent);
            }
        } catch (Exception ignored) {
            for (String agent : EvalCatalog.BUSINESS_AGENTS) {
                if (args.contains(agent)) {
                    state.spawnedAgents.add(agent);
                }
            }
        }
    }

    private static String spawnAgent(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        for (String key : SPAWN_NAME_KEYS) {
            String value = firstText(node, key, "");
            if (EvalCatalog.isBusinessAgent(value)) {
                return value;
            }
        }
        return "";
    }

    private static String agentFromSource(String source) {
        if (!StringUtils.hasText(source)) {
            return "";
        }
        int slash = source.lastIndexOf('/');
        return slash >= 0 && slash < source.length() - 1 ? source.substring(slash + 1) : source;
    }

    private static String firstText(JsonNode node, String field, String fallback) {
        if (node == null || field == null || !node.has(field) || node.get(field).isNull()) {
            return fallback == null ? "" : fallback;
        }
        JsonNode value = node.get(field);
        if (value.isTextual() || value.isNumber() || value.isBoolean()) {
            return value.asText();
        }
        return fallback == null ? "" : fallback;
    }

    private static final class ParseState {
        private final Set<String> spawnedAgents = new LinkedHashSet<>();
        private final Set<String> writeTools = new LinkedHashSet<>();
        private final List<String> interruptIds = new ArrayList<>();
        private final Map<String, String> toolNames = new LinkedHashMap<>();
        private final Map<String, StringBuilder> toolArgs = new LinkedHashMap<>();
        private final StringBuilder assistantText = new StringBuilder();
        private boolean interrupted;
        private boolean clarified;
        private String harnessError;
    }
}
