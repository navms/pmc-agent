package io.github.navms.agent.prompt;

import com.langfuse.client.prompt.PromptTemplate;
import io.github.navms.agent.observability.LangfuseProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 label 拉取 Langfuse prompt，带本地缓存与硬编码 fallback。
 *
 * @author navms
 */
@Slf4j
@Service
public class LangfusePromptService {

    private static final String DEFAULT_ASSISTANT_NAME = "何锦";

    private final LangfuseProperties properties;
    private final LangfusePromptClient client;
    private final LangfusePromptRegistry registry;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public LangfusePromptService(
            LangfuseProperties properties,
            LangfusePromptClient client,
            LangfusePromptRegistry registry) {
        this.properties = properties;
        this.client = client;
        this.registry = registry;
    }

    /**
     * 编译指定 Agent 的 system prompt，并把版本登记到 registry。
     *
     * @param prompt 目录项
     * @return 已替换变量的正文
     */
    public String compile(Prompt prompt) {
        LangfusePromptSnapshot snapshot = load(prompt);
        registry.put(prompt.getAgentName(), snapshot);
        return render(snapshot.template(), Map.of("today", LocalDate.now().toString(), "name", DEFAULT_ASSISTANT_NAME));
    }

    private LangfusePromptSnapshot load(Prompt prompt) {
        String key = prompt.getLangfuseName();
        long now = System.currentTimeMillis();
        CacheEntry cached = cache.get(key);
        int ttlMs = Math.max(0, properties.getPromptCacheTtlSeconds()) * 1000;
        if (cached != null && now - cached.loadedAt < ttlMs) {
            return cached.snapshot;
        }
        LangfusePromptSnapshot remote = fetchOrSeed(prompt);
        if (remote != null) {
            cache.put(key, new CacheEntry(remote, now));
            return remote;
        }
        if (cached != null) {
            log.warn("Langfuse prompt {} stale cache after fetch miss", key);
            return cached.snapshot;
        }
        LangfusePromptSnapshot fallback =
                new LangfusePromptSnapshot(prompt.getLangfuseName(), null, prompt.getFallbackTemplate(), true);
        cache.put(key, new CacheEntry(fallback, now));
        log.info("Using fallback prompt {}", key);
        return fallback;
    }

    private LangfusePromptSnapshot fetchOrSeed(Prompt prompt) {
        if (!properties.isLangfuseEnable()) {
            return null;
        }
        String label = properties.getPromptLabel();
        return client.fetch(prompt.getLangfuseName(), label).orElseGet(() -> {
            if (!properties.isPromptSeed()) {
                return null;
            }
            log.info("Seeding Langfuse prompt {} label={}", prompt.getLangfuseName(), label);
            client.create(prompt.getLangfuseName(), prompt.getFallbackTemplate(), label);
            return client.fetch(prompt.getLangfuseName(), label).orElse(null);
        });
    }

    static String render(String template, Map<String, String> vars) {
        if (!StringUtils.hasText(template)) {
            return "";
        }
        return PromptTemplate.compile(template, vars);
    }

    private record CacheEntry(LangfusePromptSnapshot snapshot, long loadedAt) {
    }
}
