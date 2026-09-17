package io.github.navms.agent.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 暴露 {@link LangfuseOpenTelemetryInitializer} 已注册的 OpenTelemetry，
 * 不再二次调用 {@code buildAndRegisterGlobal}。
 *
 * @author navms
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LangfuseProperties.class)
public class LangfuseOtelConfig {

    /**
     * @param properties 用于日志说明当前配置
     * @return Initializer 注册的全局实例，或 no-op
     */
    @Bean(name = "langfuseOpenTelemetry")
    public OpenTelemetry langfuseOpenTelemetry(LangfuseProperties properties) {
        OpenTelemetry otel = LangfuseOpenTelemetryInitializer.current();
        if (properties.isExportEnabled()) {
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
