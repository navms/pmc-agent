package io.github.navms.agent.observability;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 本地 / 自托管 Langfuse 上报配置。
 *
 * @author navms
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "pmc.langfuse")
public class LangfuseProperties {

    /**
     * 是否向 Langfuse 导出 OTLP traces。
     */
    private boolean enabled = true;

    /**
     * Langfuse Web 基址，例如 http://localhost:3000。
     */
    private String host = "http://localhost:3000";

    /**
     * 项目公钥。
     */
    private String publicKey;

    /**
     * 项目私钥。
     */
    private String secretKey;

    /**
     * @return 开关打开且密钥齐全时才导出
     */
    public boolean isExportEnabled() {
        return enabled && StringUtils.hasText(host) && StringUtils.hasText(publicKey) && StringUtils.hasText(secretKey);
    }

    /**
     * @return OTLP traces 端点
     */
    public String otlpTracesEndpoint() {
        String base = host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
        return base + "/api/public/otel/v1/traces";
    }

}
