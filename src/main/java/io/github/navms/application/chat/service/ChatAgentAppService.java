package io.github.navms.application.chat.service;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.navms.application.chat.dto.ResumeChatCommand;
import io.github.navms.application.chat.dto.StreamChatCommand;
import io.github.navms.config.agent.SubAgentTraceRegistry;
import io.github.navms.domain.chat.exception.BusinessException;
import io.github.navms.domain.chat.exception.ChatErrorCode;
import io.github.navms.web.agent.vo.ChatResponse;
import io.github.navms.web.agent.vo.TokenUsageVO;
import io.github.navms.web.agent.vo.message.AssistantMessageResponse;
import io.github.navms.web.agent.vo.message.MessageResponse;
import io.github.navms.web.agent.vo.message.ToolResponseMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Agent 流式执行与中断恢复。流式出参与 SSE 契约共用 {@link ChatResponse}，避免双份多态模型。
 *
 * @author navms
 */
@Slf4j
@Service
public class ChatAgentAppService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    /** Supervisor 以 Agent-as-Tool 挂载的子 Agent 名；其返回值常为自然语言终答。 */
    private static final Set<String> AGENT_TOOL_NAMES = Set.of(
            "query_bank", "summarize_bank", "export_excel", "create_chart");

    private final ReactAgent agent;

    private final ObjectMapper objectMapper;

    private final ChatSessionAppService chatSessionAppService;

    /**
     * @param agent                 Supervisor
     * @param objectMapper          JSON
     * @param chatSessionAppService 会话
     */
    public ChatAgentAppService(
            @Qualifier("supervisorAgent") ReactAgent agent,
            ObjectMapper objectMapper,
            ChatSessionAppService chatSessionAppService) {
        this.agent = agent;
        this.objectMapper = objectMapper;
        this.chatSessionAppService = chatSessionAppService;
    }

    /**
     * @param command 流式对话命令
     * @return SSE 事件
     */
    public Flux<ServerSentEvent<String>> stream(StreamChatCommand command) {
        if (command.sessionId() == null) {
            log.error("sessionId cannot be null in POST /stream");
            return Flux.error(new BusinessException(ChatErrorCode.SESSION_ID_REQUIRED));
        }
        if (!StringUtils.hasText(command.prompt())) {
            log.error("message cannot be null or empty in POST /stream for session: {}", command.sessionId());
            return Flux.error(new BusinessException(ChatErrorCode.PROMPT_REQUIRED));
        }
        try {
            chatSessionAppService.appendUserPrompt(command.sessionId(), command.prompt());
            String threadId = String.valueOf(command.sessionId());
            RunnableConfig runnableConfig = RunnableConfig.builder()
                    .threadId(threadId)
                    .addMetadata("user_id", command.userId())
                    .build();
            StreamPersistSink sink = new StreamPersistSink(command.sessionId(), chatSessionAppService, objectMapper);
            return executeAgent(new UserMessage(command.prompt()), runnableConfig, sink);
        } catch (Exception e) {
            log.error("Error during agent stream for session {}", command.sessionId(), e);
            return Flux.error(e);
        }
    }

    /**
     * @param command 恢复命令
     * @return SSE 事件
     */
    public Flux<ServerSentEvent<String>> resume(ResumeChatCommand command) {
        if (command.sessionId() == null) {
            log.warn("sessionId cannot be null in POST /resume_stream");
            return Flux.error(new BusinessException(ChatErrorCode.SESSION_ID_REQUIRED));
        }
        try {
            chatSessionAppService.requireSession(command.sessionId());
            InterruptionMetadata.Builder metadataBuilder = InterruptionMetadata.builder();
            if (!CollectionUtils.isEmpty(command.toolFeedbacks())) {
                metadataBuilder.toolFeedbacks(command.toolFeedbacks());
            }
            String threadId = String.valueOf(command.sessionId());
            RunnableConfig runnableConfig = RunnableConfig.builder()
                    .threadId(threadId)
                    .addMetadata("user_id", command.userId())
                    .addHumanFeedback(metadataBuilder.build())
                    .build();
            StreamPersistSink sink = new StreamPersistSink(command.sessionId(), chatSessionAppService, objectMapper);
            return executeAgent(null, runnableConfig, sink);
        } catch (Exception e) {
            log.error("Error during agent resume for session {}", command.sessionId(), e);
            return Flux.error(e);
        }
    }

    private Flux<ServerSentEvent<String>> executeAgent(
            UserMessage userMessage,
            RunnableConfig runnableConfig,
            StreamPersistSink sink) throws GraphRunnerException {
        Flux<NodeOutput> agentStream;
        if (userMessage != null) {
            agentStream = agent.stream(userMessage, runnableConfig);
        } else {
            agentStream = agent.stream("", runnableConfig);
        }

        // 框架 CompiledGraph.streamFromInitialNode 用 flatMap 拆 GraphResponse，chunk 会在多线程乱序到达
        // （见 spring-ai-alibaba#4630 / #4316）。publishOn(single) 按订阅顺序串行，避免前端拼字错乱。
        //
        // AGENT_MODEL_FINISHED 通常是 STREAMING 的聚合，直接下发会重复。
        // 但 Tool 之后若本轮没有 STREAMING 增量（只下发了 FINISHED），过滤掉就会导致「Tool 后无输出」。
        AtomicBoolean sawModelStreaming = new AtomicBoolean(false);
        AtomicReference<String> lastPromotedNarrative = new AtomicReference<>();
        return agentStream
                .publishOn(Schedulers.single())
                .concatMap(nodeOutput -> {
                    String node = nodeOutput.node();
                    String agentName = nodeOutput.agent();
                    TokenUsageVO tokenUsage = TokenUsageVO.from(nodeOutput.tokenUsage());
                    List<ChatResponse> responses = new ArrayList<>();

                    if (nodeOutput instanceof StreamingOutput<?> streamingOutput) {
                        Message message = streamingOutput.message();
                        if (message == null) {
                            addUsageOnly(responses, node, agentName, tokenUsage);
                        } else {
                            OutputType outputType = streamingOutput.getOutputType();
                            if (outputType == OutputType.AGENT_MODEL_FINISHED) {
                                // 本轮已有增量流，跳过聚合帧，避免重复；仍转发 usage
                                if (sawModelStreaming.getAndSet(false)) {
                                    addUsageOnly(responses, node, agentName, tokenUsage);
                                } else if (!(message instanceof AssistantMessage finished)
                                        || finished.hasToolCalls()
                                        || !StringUtils.hasText(finished.getText())) {
                                    addUsageOnly(responses, node, agentName, tokenUsage);
                                } else {
                                    String finishedText = finished.getText();
                                    // 刚把 AgentTool 自然语言提升过时，Supervisor 的 FINISHED 常是同文复述
                                    String promoted = lastPromotedNarrative.getAndSet(null);
                                    if (promoted != null && promoted.equals(finishedText)) {
                                        addUsageOnly(responses, node, agentName, tokenUsage);
                                    } else {
                                        responses.add(new ChatResponse(
                                                node,
                                                agentName,
                                                MessageResponse.MessageFactory.fromMessage(message),
                                                tokenUsage,
                                                finishedText));
                                    }
                                }
                            } else {
                                String chunk = "";
                                if (message instanceof AssistantMessage assistantMessage && !assistantMessage.hasToolCalls()) {
                                    chunk = assistantMessage.getText();
                                    if (StringUtils.hasText(chunk) && outputType == OutputType.AGENT_MODEL_STREAMING) {
                                        sawModelStreaming.set(true);
                                        lastPromotedNarrative.set(null);
                                    }
                                } else {
                                    // 进入 tool-request / tool 等，重置本轮 streaming 标记
                                    sawModelStreaming.set(false);
                                }
                                MessageResponse messageResponse = MessageResponse.MessageFactory.fromMessage(message);
                                String promotedNarrative = null;
                                if (messageResponse instanceof ToolResponseMessageResponse toolResponse) {
                                    // 先展开子 Agent 内部 tool 轨迹，再压缩 AgentTool 终答
                                    responses.addAll(expandSubAgentToolTraces(toolResponse));
                                    promotedNarrative = promoteAndCompactAgentToolNarratives(toolResponse);
                                }
                                responses.add(new ChatResponse(
                                        node,
                                        agentName,
                                        messageResponse,
                                        tokenUsage,
                                        chunk));
                                if (StringUtils.hasText(promotedNarrative)) {
                                    lastPromotedNarrative.set(promotedNarrative);
                                    // 提升为助手气泡：带 chunk 以便前端流式追加、落库走 flushAssistant
                                    responses.add(new ChatResponse(
                                            node,
                                            agentName,
                                            new AssistantMessageResponse(promotedNarrative),
                                            null,
                                            promotedNarrative));
                                }
                            }
                        }
                    } else if (nodeOutput instanceof InterruptionMetadata interruptionMetadata) {
                        sawModelStreaming.set(false);
                        lastPromotedNarrative.set(null);
                        ChatResponse interrupted = new ChatResponse(
                                node,
                                agentName,
                                MessageResponse.MessageFactory.fromInterruptionMetadata(interruptionMetadata),
                                tokenUsage,
                                "");
                        interrupted.setInterrupted(true);
                        responses.add(interrupted);
                    }

                    if (responses.isEmpty()) {
                        return Flux.just(emptySse());
                    }
                    List<ServerSentEvent<String>> events = new ArrayList<>(responses.size());
                    for (ChatResponse agentResponse : responses) {
                        try {
                            sink.onResponse(agentResponse);
                            events.add(ServerSentEvent.<String>builder()
                                    .data(objectMapper.writeValueAsString(agentResponse))
                                    .build());
                        } catch (Exception e) {
                            log.error("Failed to serialize AgentRunResponse to JSON", e);
                            events.add(ServerSentEvent.<String>builder()
                                    .data("{\"error\":\"Failed to serialize response\"}")
                                    .build());
                        }
                    }
                    return Flux.fromIterable(events);
                })
                .doOnComplete(sink::onComplete)
                .onErrorResume(error -> {
                    log.error("Error occurred during agent stream execution", error);
                    String data;
                    try {
                        data = objectMapper.writeValueAsString(error);
                    } catch (Exception e) {
                        log.error("Failed to create error SSE event", e);
                        data = "{\"error\":true,\"errorMessage\":\"Internal error occurred\"}";
                    }
                    return Flux.just(ServerSentEvent.<String>builder().event("error").data(data).build());
                });
    }

    /**
     * 从 {@link SubAgentTraceRegistry} 取出子 Agent 内部 tool 轨迹，展开为独立 SSE 消息。
     *
     * @param toolResponse AgentTool 的 tool 结果
     * @return 嵌套 tool-request / tool 事件
     */
    private List<ChatResponse> expandSubAgentToolTraces(ToolResponseMessageResponse toolResponse) {
        if (toolResponse == null || CollectionUtils.isEmpty(toolResponse.getResponses())) {
            return List.of();
        }
        List<ChatResponse> nested = new ArrayList<>();
        for (ToolResponseMessageResponse.ToolResponse response : toolResponse.getResponses()) {
            if (response == null || !AGENT_TOOL_NAMES.contains(response.getName())) {
                continue;
            }
            String subAgentName = response.getName();
            for (MessageResponse nestedMessage : SubAgentTraceRegistry.poll(subAgentName)) {
                nested.add(new ChatResponse(
                        "_SUB_AGENT_TOOL_",
                        subAgentName,
                        nestedMessage,
                        null,
                        ""));
            }
        }
        return nested;
    }

    /**
     * Agent-as-Tool 会把子 Agent 终答文本塞进 tool.responseData。
     * 将其提升为助手消息，并把工具块压缩为占位，避免 UI 把整段报告当工具输出展示。
     *
     * @param toolResponse 工具消息
     * @return 需提升的自然语言；无可提升内容时返回 null
     */
    private String promoteAndCompactAgentToolNarratives(ToolResponseMessageResponse toolResponse) {
        if (toolResponse == null || CollectionUtils.isEmpty(toolResponse.getResponses())) {
            return null;
        }
        StringBuilder narratives = new StringBuilder();
        for (ToolResponseMessageResponse.ToolResponse response : toolResponse.getResponses()) {
            if (response == null || !AGENT_TOOL_NAMES.contains(response.getName())) {
                continue;
            }
            String data = response.getResponseData();
            if (!isAgentToolNarrative(data)) {
                continue;
            }
            if (!narratives.isEmpty()) {
                narratives.append("\n\n");
            }
            narratives.append(data);
            response.setResponseData(compactAgentToolData(data));
        }
        return narratives.isEmpty() ? null : narratives.toString();
    }

    private boolean isAgentToolNarrative(String data) {
        if (!StringUtils.hasText(data)) {
            return false;
        }
        String trimmed = data.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                Map<String, Object> map = objectMapper.readValue(trimmed, MAP_TYPE);
                if (map.containsKey("success")
                        || map.containsKey("data")
                        || map.containsKey("downloadUrl")
                        || map.containsKey("option")
                        || map.containsKey("_promoted")) {
                    return false;
                }
            } catch (Exception ignored) {
                // 夹杂 Markdown 的散文，按自然语言提升
            }
        }
        return true;
    }

    private String compactAgentToolData(String data) {
        String artifactJson = extractEmbeddedArtifactJson(data);
        if (artifactJson != null) {
            return artifactJson;
        }
        return "{\"_promoted\":true,\"note\":\"子Agent回复已作为助手消息展示\",\"chars\":"
                + data.length() + "}";
    }

    private String extractEmbeddedArtifactJson(String data) {
        if (!StringUtils.hasText(data)) {
            return null;
        }
        int start = data.indexOf('{');
        int end = data.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        String candidate = data.substring(start, end + 1);
        try {
            Map<String, Object> map = objectMapper.readValue(candidate, MAP_TYPE);
            if (map.containsKey("downloadUrl") || map.containsKey("option")) {
                return candidate;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static void addUsageOnly(
            List<ChatResponse> responses, String node, String agentName, TokenUsageVO tokenUsage) {
        if (tokenUsage != null) {
            responses.add(new ChatResponse(node, agentName, null, tokenUsage, ""));
        }
    }

    private static ServerSentEvent<String> emptySse() {
        return ServerSentEvent.<String>builder().data("{}").build();
    }

    /**
     * 将完整消息落入 chat_message，忽略 chunk 分片。
     *
     * @author navms
     */
    static final class StreamPersistSink {

        private final Long sessionId;

        private final ChatSessionAppService chatSessionAppService;

        private final ObjectMapper objectMapper;

        private final StringBuilder assistant = new StringBuilder();

        private TokenUsageVO pendingUsage;

        private boolean interrupted;

        StreamPersistSink(Long sessionId, ChatSessionAppService chatSessionAppService, ObjectMapper objectMapper) {
            this.sessionId = sessionId;
            this.chatSessionAppService = chatSessionAppService;
            this.objectMapper = objectMapper;
        }

        void onResponse(ChatResponse response) {
            if (response == null) {
                return;
            }
            MessageResponse message = response.getMessageResponse();
            if (response.isInterrupted()) {
                TokenUsageVO flushedUsage = flushAssistant();
                interrupted = true;
                if (message != null) {
                    persistMessage(
                            message,
                            null,
                            flushedUsage != null ? null : response.getTokenUsage());
                }
                chatSessionAppService.markStatus(sessionId, "interrupted");
                return;
            }
            if (message == null) {
                rememberUsage(response.getTokenUsage());
                appendChunk(response.getChunk());
                return;
            }
            String type = message.getMessageType();
            if ("user".equals(type)) {
                return;
            }
            if ("tool-request".equals(type) || "tool".equals(type) || "tool-confirm".equals(type)) {
                TokenUsageVO flushedUsage = flushAssistant();
                TokenUsageVO forThis = flushedUsage != null ? null : response.getTokenUsage();
                persistMessage(message, response.getAgentName(), forThis);
                if (forThis != null) {
                    pendingUsage = null;
                }
                return;
            }
            if ("assistant".equals(type)) {
                rememberUsage(response.getTokenUsage());
                if (StringUtils.hasText(response.getChunk())) {
                    appendChunk(response.getChunk());
                } else if (StringUtils.hasText(message.getContent()) && assistant.isEmpty()) {
                    persistMessage(message, response.getAgentName(), takePendingUsage(response.getTokenUsage()));
                }
            }
        }

        void onComplete() {
            flushAssistant();
            if (!interrupted) {
                chatSessionAppService.markStatus(sessionId, "active");
            }
        }

        private void persistMessage(MessageResponse message, String agentName, TokenUsageVO tokenUsage) {
            Map<String, Object> converted = objectMapper.convertValue(message, MAP_TYPE);
            Map<String, Object> payload = converted == null ? new HashMap<>() : new HashMap<>(converted);
            if (StringUtils.hasText(agentName)) {
                payload.put("agentName", agentName);
            }
            Map<String, Object> usageMap = toUsageMap(tokenUsage);
            if (usageMap != null) {
                payload.put("tokenUsage", usageMap);
            }
            String content = message.getContent() == null ? "" : message.getContent();
            chatSessionAppService.appendMessage(sessionId, message.getMessageType(), content, payload);
        }

        private void appendChunk(String chunk) {
            if (!StringUtils.hasText(chunk)) {
                return;
            }
            // 上游已 publishOn(single) 保序，chunk 为纯增量，直接追加
            assistant.append(chunk);
        }

        private TokenUsageVO flushAssistant() {
            if (assistant.isEmpty()) {
                return null;
            }
            TokenUsageVO usage = pendingUsage;
            chatSessionAppService.appendAssistantText(sessionId, assistant.toString(), toUsageMap(usage));
            assistant.setLength(0);
            pendingUsage = null;
            return usage;
        }

        private void rememberUsage(TokenUsageVO tokenUsage) {
            if (tokenUsage != null) {
                pendingUsage = tokenUsage;
            }
        }

        private TokenUsageVO takePendingUsage(TokenUsageVO fallback) {
            TokenUsageVO usage = pendingUsage != null ? pendingUsage : fallback;
            pendingUsage = null;
            return usage;
        }

        private Map<String, Object> toUsageMap(TokenUsageVO tokenUsage) {
            if (tokenUsage == null) {
                return null;
            }
            return objectMapper.convertValue(tokenUsage, MAP_TYPE);
        }
    }
}
