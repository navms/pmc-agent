package io.github.navms.agent.observability;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 在任何 Bean（含 Hikari / MySQL）创建之前注册 GlobalOpenTelemetry。
 *
 * <p>OpenTelemetry 官方要求尽早、只调用一次 {@code set}/{@code buildAndRegisterGlobal}；
 * 一旦有库先调用了 {@link GlobalOpenTelemetry#get()}，后续注册会失败。
 *
 * @author navms
 */
public class LangfuseOpenTelemetryInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static OpenTelemetry openTelemetry;
    private static SdkTracerProvider tracerProvider;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        if (openTelemetry != null) {
            return;
        }
        Environment env = applicationContext.getEnvironment();
        boolean enabled = env.getProperty("pmc.langfuse.enabled", Boolean.class, true);
        String host = env.getProperty("pmc.langfuse.host", "http://localhost:3000");
        String publicKey = env.getProperty("pmc.langfuse.public-key");
        String secretKey = env.getProperty("pmc.langfuse.secret-key");
        if (!enabled || !StringUtils.hasText(host) || !StringUtils.hasText(publicKey) || !StringUtils.hasText(secretKey)) {
            openTelemetry = OpenTelemetry.noop();
            return;
        }
        String base = host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
        String endpoint = base + "/api/public/otel/v1/traces";
        String encoded = Base64.getEncoder()
                .encodeToString((publicKey + ":" + secretKey).getBytes(StandardCharsets.UTF_8));

        OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Basic " + encoded)
                .addHeader("x-langfuse-ingestion-version", "4")
                .build();
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), "pmc-agent")));
        SdkTracerProvider provider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build()).setResource(resource).build();

        openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(provider).buildAndRegisterGlobal();
        tracerProvider = provider;
    }

    static OpenTelemetry current() {
        return openTelemetry == null ? OpenTelemetry.noop() : openTelemetry;
    }

    static SdkTracerProvider tracerProvider() {
        return tracerProvider;
    }

}
