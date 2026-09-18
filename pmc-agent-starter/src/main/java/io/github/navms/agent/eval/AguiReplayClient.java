package io.github.navms.agent.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用现网 AG-UI HTTP 接口跑一轮用户消息或 HITL resume。
 *
 * @author navms
 */
@Slf4j
@Component
public class AguiReplayClient {

    private final ObjectMapper objectMapper;

    private final AguiSseParser parser;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public AguiReplayClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.parser = new AguiSseParser(objectMapper);
    }

    /**
     * @param baseUrl
     * @param sessionId 会话
     * @param userId    用户
     * @param content   用户正文
     * @param timeout   超时
     * @return 观察
     */
    public TranscriptObservation sendUser(
            String baseUrl, long sessionId, String userId, String content, Duration timeout) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", UUID.randomUUID().toString());
        user.put("role", "user");
        user.put("content", content);
        Map<String, Object> body = baseBody(sessionId);
        body.put("messages", List.of(user));
        return post(baseUrl, sessionId, userId, body, timeout);
    }

    /**
     * @param interruptIds RUN_FINISHED.interrupts[].id
     * @param approved     是否批准
     */
    public TranscriptObservation resume(
            String baseUrl,
            long sessionId,
            String userId,
            List<String> interruptIds,
            boolean approved,
            Duration timeout) {
        List<Map<String, Object>> resume = new ArrayList<>();
        for (String id : interruptIds) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("interruptId", id);
            entry.put("status", "resolved");
            entry.put("payload", Map.of("approved", approved));
            resume.add(entry);
        }
        Map<String, Object> body = baseBody(sessionId);
        body.put("messages", List.of());
        body.put("resume", resume);
        return post(baseUrl, sessionId, userId, body, timeout);
    }

    private Map<String, Object> baseBody(long sessionId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("threadId", String.valueOf(sessionId));
        body.put("runId", UUID.randomUUID().toString());
        return body;
    }

    private TranscriptObservation post(
            String baseUrl, long sessionId, String userId, Map<String, Object> body, Duration timeout) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder(URI.create(trimSlash(baseUrl) + "/agui/run"))
                    .timeout(timeout)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Accept", "text/event-stream")
                    .header("X-User-Id", userId)
                    .header("X-Agent-Id", "pmc_supervisor")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 300) {
                return error("AG-UI HTTP " + response.statusCode() + " session=" + sessionId);
            }
            try (InputStream in = response.body();
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return parser.parse(reader);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("AG-UI interrupted session=" + sessionId);
        } catch (Exception e) {
            log.warn("AG-UI replay failed session={}: {}", sessionId, e.getMessage());
            return error("AG-UI " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static TranscriptObservation error(String message) {
        return new TranscriptObservation(java.util.Set.of(), java.util.Set.of(), false, List.of(), false, "", message, 0L);
    }

    private static String trimSlash(String baseUrl) {
        if (baseUrl == null) {
            return "http://127.0.0.1:8080";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
