package io.github.navms.agent.middle;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentStartEvent;
import io.agentscope.core.event.ModelCallEndEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.event.ToolResultStartEvent;
import io.agentscope.core.event.ToolResultTextDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ModelCallInput;
import io.agentscope.core.model.Model;
import io.agentscope.core.util.JsonUtils;
import io.github.navms.agent.prompt.LangfusePromptRegistry;
import io.github.navms.agent.prompt.LangfusePromptSnapshot;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.reactor.v3_1.ContextPropagationOperator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AgentScope 生命周期 OTEL span（与 {@code OtelTracingMiddleware} 对齐），并写入 Langfuse
 * session/user 以及 observation input/output。
 *
 * @author navms
 */
public class LangfuseSessionMiddleware implements MiddlewareBase {

    private static final String INSTRUMENTATION_NAME = "io.agentscope";

    private static volatile boolean hookRegistered = false;

    private final LangfusePromptRegistry promptRegistry;

    public LangfuseSessionMiddleware(LangfusePromptRegistry promptRegistry) {
        this.promptRegistry = promptRegistry;
        if (!hookRegistered) {
            synchronized (LangfuseSessionMiddleware.class) {
                if (!hookRegistered) {
                    ContextPropagationOperator.builder().build().registerOnEachOperator();
                    hookRegistered = true;
                }
            }
        }
    }

    private Tracer getTracer() {
        return GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME);
    }

    @Override
    public Flux<AgentEvent> onAgent(
            Agent agent,
            RuntimeContext ctx,
            AgentInput input,
            Function<AgentInput, Flux<AgentEvent>> next) {
        return Flux.deferContextual(ctxView -> {
            Context parentContext = resolveOtelContext(ctxView);

            Span span = getTracer()
                    .spanBuilder("invoke_agent " + agent.getName())
                    .setParent(parentContext)
                    .setAttribute("gen_ai.operation.name", "invoke_agent")
                    .setAttribute("gen_ai.agent.name", agent.getName())
                    .setAttribute("gen_ai.agent.id", agent.getAgentId() != null ? agent.getAgentId() : "")
                    .setAttribute("gen_ai.request.messages.count", input.msgs() != null ? (long) input.msgs().size() : 0L)
                    .startSpan();
            applySessionAttributes(span, ctx);
            applyPromptAttributes(span, agent.getName());
            applyObservationInput(span, serializeMessages(input.msgs()));

            Context otelCtx = span.storeInContext(parentContext);
            AtomicReference<Boolean> ended = new AtomicReference<>(false);
            StringBuilder textBuf = new StringBuilder();
            StringBuilder thinkingBuf = new StringBuilder();

            return ContextPropagationOperator.runWithContext(next.apply(input)
                    .doOnNext(event -> {
                        if (event instanceof AgentStartEvent rse
                                && rse.getSource() == null
                                && rse.getReplyId() != null) {
                            span.setAttribute("agentscope.agent.reply_id", rse.getReplyId());
                        } else if (event instanceof ThinkingBlockDeltaEvent tbd && tbd.getDelta() != null) {
                            thinkingBuf.append(tbd.getDelta());
                        } else if (event instanceof TextBlockDeltaEvent tbd && tbd.getDelta() != null) {
                            textBuf.append(tbd.getDelta());
                        }
                    }).doOnComplete(() -> {
                        if (ended.compareAndSet(false, true)) {
                            applyObservationOutput(span, serializeModelOutput(
                                    textBuf.toString(), thinkingBuf.toString(), List.of()));
                            span.setStatus(StatusCode.OK);
                            span.end();
                        }
                    }).doOnError(e -> {
                        if (ended.compareAndSet(false, true)) {
                            applyObservationOutput(span, serializeModelOutput(
                                    textBuf.toString(), thinkingBuf.toString(), List.of()));
                            span.setStatus(StatusCode.ERROR, e.getMessage());
                            span.recordException(e);
                            span.end();
                        }
                    }).doOnCancel(() -> {
                        if (ended.compareAndSet(false, true)) {
                            span.setStatus(StatusCode.ERROR, "cancelled");
                            span.end();
                        }
                    }), otelCtx);
        });
    }

    @Override
    public Flux<AgentEvent> onModelCall(
            Agent agent,
            RuntimeContext ctx,
            ModelCallInput input,
            Function<ModelCallInput, Flux<AgentEvent>> next) {
        return Flux.deferContextual(ctxView -> {
            Context parentContext = resolveOtelContext(ctxView);
            Model model = input.model();
            String modelName = model != null ? model.getModelName() : "unknown";
            Span span = getTracer()
                    .spanBuilder("chat " + modelName)
                    .setParent(parentContext)
                    .setAttribute("gen_ai.operation.name", "chat")
                    .setAttribute("gen_ai.request.model", modelName)
                    .setAttribute("gen_ai.request.messages.count", input.messages() != null ? (long) input.messages().size() : 0L)
                    .setAttribute("gen_ai.request.tools.count", input.tools() != null ? (long) input.tools().size() : 0L)
                    .startSpan();
            applySessionAttributes(span, ctx);
            applyPromptAttributes(span, agent.getName());
            // Langfuse Preview 映射：gen_ai.prompt / langfuse.observation.input
            applyObservationInput(span, serializeMessages(input.messages()));

            Context otelCtx = span.storeInContext(parentContext);
            AtomicReference<Boolean> ended = new AtomicReference<>(false);
            StringBuilder textBuf = new StringBuilder();
            StringBuilder thinkingBuf = new StringBuilder();
            List<Map<String, String>> toolCalls = new ArrayList<>();

            return ContextPropagationOperator.runWithContext(next.apply(input)
                    .doOnNext(event -> {
                        if (event instanceof ThinkingBlockDeltaEvent tbd && tbd.getDelta() != null) {
                            thinkingBuf.append(tbd.getDelta());
                        } else if (event instanceof TextBlockDeltaEvent tbd && tbd.getDelta() != null) {
                            textBuf.append(tbd.getDelta());
                        } else if (event instanceof ToolCallStartEvent tcs) {
                            Map<String, String> call = new LinkedHashMap<>();
                            call.put("id", tcs.getToolCallId() == null ? "" : tcs.getToolCallId());
                            call.put("name", tcs.getToolCallName() == null ? "" : tcs.getToolCallName());
                            toolCalls.add(call);
                        } else if (event instanceof ModelCallEndEvent mce) {
                            setModelResponseAttributes(span, mce);
                        }
                    }).doOnComplete(() -> {
                        if (ended.compareAndSet(false, true)) {
                            applyObservationOutput(
                                    span,
                                    serializeModelOutput(textBuf.toString(), thinkingBuf.toString(), toolCalls));
                            span.setStatus(StatusCode.OK);
                            span.end();
                        }
                    }).doOnError(e -> {
                        if (ended.compareAndSet(false, true)) {
                            applyObservationOutput(
                                    span,
                                    serializeModelOutput(textBuf.toString(), thinkingBuf.toString(), toolCalls));
                            span.setStatus(StatusCode.ERROR, e.getMessage());
                            span.recordException(e);
                            span.end();
                        }
                    }).doOnCancel(() -> {
                        if (ended.compareAndSet(false, true)) {
                            span.setStatus(StatusCode.ERROR, "cancelled");
                            span.end();
                        }
                    }), otelCtx);
        });
    }

    @Override
    public Flux<AgentEvent> onActing(
            Agent agent,
            RuntimeContext ctx,
            ActingInput input,
            Function<ActingInput, Flux<AgentEvent>> next) {
        return Flux.deferContextual(ctxView -> {
            Context parentContext = resolveOtelContext(ctxView);
            String toolNames = input.toolCalls() != null ? input.toolCalls().stream()
                    .map(ToolUseBlock::getName).collect(Collectors.joining(", ")) : "unknown";
            String spanName = buildToolSpanName(input);

            Span span = getTracer()
                    .spanBuilder("execute_tool " + spanName)
                    .setParent(parentContext)
                    .setAttribute("gen_ai.operation.name", "execute_tool")
                    .setAttribute("gen_ai.tool.name", toolNames)
                    .setAttribute("gen_ai.tool.call.count", input.toolCalls() != null ? (long) input.toolCalls().size() : 0L)
                    .startSpan();
            applySessionAttributes(span, ctx);
            applyPromptAttributes(span, agent.getName());
            applyObservationInput(span, serializeToolCalls(input.toolCalls()));

            Context otelCtx = span.storeInContext(parentContext);
            AtomicReference<Boolean> ended = new AtomicReference<>(false);
            Set<String> callIds = ConcurrentHashMap.newKeySet();
            Map<String, String> toolNamesById = new ConcurrentHashMap<>();
            Map<String, StringBuilder> toolText = new ConcurrentHashMap<>();

            return ContextPropagationOperator.runWithContext(next.apply(input)
                    .doOnNext(event -> {
                        if (event instanceof ToolResultStartEvent start) {
                            if (start.getToolCallId() != null) {
                                callIds.add(start.getToolCallId());
                                toolNamesById.put(start.getToolCallId(), start.getToolCallName() == null ? "" : start.getToolCallName());
                                toolText.computeIfAbsent(start.getToolCallId(), k -> new StringBuilder());
                            }
                        } else if (event instanceof ToolResultTextDeltaEvent delta) {
                            if (delta.getToolCallId() != null && delta.getDelta() != null) {
                                toolText.computeIfAbsent(delta.getToolCallId(), k -> new StringBuilder()).append(delta.getDelta());
                            }
                        } else if (event instanceof ToolResultEndEvent tre && tre.getToolCallId() != null) {
                            callIds.add(tre.getToolCallId());
                        }
                    }).doOnComplete(() -> {
                        if (ended.compareAndSet(false, true)) {
                            setToolCallIds(span, callIds);
                            applyObservationOutput(span, serializeToolResults(toolNamesById, toolText));
                            span.setStatus(StatusCode.OK);
                            span.end();
                        }
                    }).doOnError(e -> {
                        if (ended.compareAndSet(false, true)) {
                            setToolCallIds(span, callIds);
                            applyObservationOutput(span, serializeToolResults(toolNamesById, toolText));
                            span.setStatus(StatusCode.ERROR, e.getMessage());
                            span.recordException(e);
                            span.end();
                        }
                    }).doOnCancel(() -> {
                        if (ended.compareAndSet(false, true)) {
                            setToolCallIds(span, callIds);
                            span.setStatus(StatusCode.ERROR, "cancelled");
                            span.end();
                        }
                    }), otelCtx);
        });
    }

    private static void applySessionAttributes(Span span, RuntimeContext ctx) {
        if (ctx == null) {
            return;
        }
        if (StringUtils.hasText(ctx.getUserId())) {
            span.setAttribute("langfuse.user.id", ctx.getUserId());
            span.setAttribute("user.id", ctx.getUserId());
        }
        if (StringUtils.hasText(ctx.getSessionId())) {
            span.setAttribute("langfuse.session.id", ctx.getSessionId());
            span.setAttribute("session.id", ctx.getSessionId());
        }
    }

    private void applyPromptAttributes(Span span, String agentName) {
        if (promptRegistry == null || !StringUtils.hasText(agentName)) {
            return;
        }
        promptRegistry.find(agentName).ifPresent(snapshot -> applyPromptSnapshot(span, snapshot));
    }

    private static void applyPromptSnapshot(Span span, LangfusePromptSnapshot snapshot) {
        if (StringUtils.hasText(snapshot.name())) {
            span.setAttribute("langfuse.observation.prompt.name", snapshot.name());
        }
        if (snapshot.version() != null) {
            span.setAttribute("langfuse.observation.prompt.version", (long) snapshot.version());
        }
    }

    private static void applyObservationInput(Span span, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        span.setAttribute("langfuse.observation.input", value);
        span.setAttribute("gen_ai.prompt", value);
        span.setAttribute("input.value", value);
    }

    private static void applyObservationOutput(Span span, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        span.setAttribute("langfuse.observation.output", value);
        span.setAttribute("gen_ai.completion", value);
        span.setAttribute("output.value", value);
    }

    private static String serializeMessages(List<Msg> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return null;
        }

        List<Map<String, Object>> rows = new ArrayList<>(messages.size());
        for (Msg msg : messages) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("role", msg.getRole() != null ? msg.getRole().name() : "UNKNOWN");
            String text = msg.getTextContent();
            if (StringUtils.hasText(text)) {
                row.put("content", text);
            }
            String thinking = joinThinking(msg);
            if (StringUtils.hasText(thinking)) {
                row.put("reasoning", thinking);
            }
            List<ToolUseBlock> toolUses = msg.getContentBlocks(ToolUseBlock.class);
            if (!CollectionUtils.isEmpty(toolUses)) {
                row.put("tool_calls", buildToolCalls(toolUses));
            }
            rows.add(row);
        }
        return JsonUtils.getJsonCodec().toJson(rows);
    }

    private static List<Map<String, Object>> buildToolCalls(List<ToolUseBlock> toolUses) {
        List<Map<String, Object>> calls = new ArrayList<>(toolUses.size());
        for (ToolUseBlock tu : toolUses) {
            Map<String, Object> call = new LinkedHashMap<>();
            call.put("id", tu.getId());
            call.put("name", tu.getName());
            call.put("arguments", tu.getInput() != null ? tu.getInput() : Map.of());
            calls.add(call);
        }
        return calls;
    }

    static String serializeModelOutput(String text, String reasoning, List<Map<String, String>> toolCalls) {
        boolean hasText = StringUtils.hasText(text);
        boolean hasReasoning = StringUtils.hasText(reasoning);
        boolean hasTools = toolCalls != null && !toolCalls.isEmpty();
        if (!hasText && !hasReasoning && !hasTools) {
            return null;
        }
        if (hasText && !hasReasoning && !hasTools) {
            return text;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        if (hasReasoning) {
            body.put("reasoning", reasoning);
        }
        if (hasText) {
            body.put("text", text);
        }
        if (hasTools) {
            body.put("tool_calls", toolCalls);
        }
        return JsonUtils.getJsonCodec().toJson(body);
    }

    private static String joinThinking(Msg msg) {
        List<ThinkingBlock> blocks = msg.getContentBlocks(ThinkingBlock.class);
        if (CollectionUtils.isEmpty(blocks)) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        for (ThinkingBlock block : blocks) {
            if (block != null && StringUtils.hasText(block.getThinking())) {
                buf.append(block.getThinking());
            }
        }
        return buf.isEmpty() ? null : buf.toString();
    }

    private static String serializeToolCalls(List<ToolUseBlock> toolCalls) {
        if (CollectionUtils.isEmpty(toolCalls)) {
            return null;
        }
        return JsonUtils.getJsonCodec().toJson(buildToolCalls(toolCalls));
    }

    private static String serializeToolResults(
            Map<String, String> toolNamesById, Map<String, StringBuilder> toolText) {
        if (toolText == null || toolText.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> rows = new ArrayList<>(toolText.size());
        for (Map.Entry<String, StringBuilder> e : toolText.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", e.getKey());
            row.put("name", toolNamesById.getOrDefault(e.getKey(), "<unknown>"));
            row.put("result", e.getValue().toString());
            rows.add(row);
        }
        return JsonUtils.getJsonCodec().toJson(rows);
    }

    private Context resolveOtelContext(ContextView ctxView) {
        return ContextPropagationOperator.getOpenTelemetryContextFromContextView(
                ctxView, Context.current());
    }

    private static String buildToolSpanName(ActingInput input) {
        if (CollectionUtils.isEmpty(input.toolCalls())) {
            return "UNKNOWN";
        }
        String first = input.toolCalls().getFirst().getName();
        int rest = input.toolCalls().size() - 1;
        return rest > 0 ? first + " (+" + rest + " more)" : first;
    }

    private void setToolCallIds(Span span, Set<String> callIds) {
        if (!callIds.isEmpty()) {
            span.setAttribute("gen_ai.tool.call.id", String.join(",", callIds));
        }
    }

    private void setModelResponseAttributes(Span span, ModelCallEndEvent event) {
        if (event.getUsage() != null) {
            var usage = event.getUsage();
            span.setAttribute("gen_ai.usage.input_tokens", usage.getInputTokens());
            span.setAttribute("gen_ai.usage.output_tokens", usage.getOutputTokens());
        }
    }

}
