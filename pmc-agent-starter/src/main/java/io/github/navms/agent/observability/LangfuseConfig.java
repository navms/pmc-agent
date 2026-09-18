package io.github.navms.agent.observability;

import com.langfuse.client.LangfuseClient;
import io.github.navms.agent.prompt.LangfusePromptRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Langfuse OTEL 导出与官方 API Client（Prompt 等）。
 *
 * @author navms
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LangfuseProperties.class)
public class LangfuseConfig {

    @Bean
    public LangfusePromptRegistry langfusePromptRegistry() {
        return new LangfusePromptRegistry();
    }

    /**
     * 官方 langfuse-java API 客户端；开关打开且密钥齐全时才创建。
     *
     * @param properties 连接配置
     * @return LangfuseClient
     */
    @Bean
    @ConditionalOnProperty(prefix = "pmc.langfuse", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${pmc.langfuse.public-key:}')"
            + " && T(org.springframework.util.StringUtils).hasText('${pmc.langfuse.secret-key:}')")
    public LangfuseClient langfuseClient(LangfuseProperties properties) {
        String url = properties.apiBaseUrl();
        log.info("Langfuse API client -> {}", url);
        return LangfuseClient.builder().url(url).credentials(properties.getPublicKey(), properties.getSecretKey()).timeout(15).build();
    }

    /**
     * @param properties 用于日志说明当前配置
     * @return Initializer 注册的全局实例，或 no-op
     */
    @Bean(name = "langfuseOpenTelemetry")
    public OpenTelemetry langfuseOpenTelemetry(LangfuseProperties properties) {
        OpenTelemetry otel = LangfuseOpenTelemetryInitializer.current();
        if (properties.isLangfuseEnable()) {
            log.info("Langfuse OTLP export -> {}", properties.otlpTracesEndpoint());
        } else {
            log.info("Langfuse OTLP export disabled");
        }
        return otel;
    }

    @PreDestroy
    void shutdown() {
        SdkTracerProvider provider = LangfuseOpenTelemetryInitializer.tracerProvider();
        if (provider != null) {
            provider.shutdown().join(10, TimeUnit.SECONDS);
        }
    }
}
