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
     * Langfuse Web 基址
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
     * 运行时拉取的 prompt label，默认 production。
     */
    private String promptLabel = "production";

    /**
     * 客户端缓存秒数，对齐 Langfuse SDK 默认 60s。
     */
    private int promptCacheTtlSeconds = 60;

    /**
     * 远端不存在时是否用本地 fallback 正文 seed 一份并打上 promptLabel。
     */
    private boolean promptSeed = true;

    /**
     * 点踩写入的 Langfuse Dataset 名（base case）。
     */
    private String baseCaseDataset = "base-case";

    /**
     * @return Langfuse 是否可用
     */
    public boolean isLangfuseEnable() {
        return enabled && StringUtils.hasText(host) && StringUtils.hasText(publicKey) && StringUtils.hasText(secretKey);
    }

    /**
     * @return OTLP traces 端点
     */
    public String otlpTracesEndpoint() {
        return apiBaseUrl() + "/api/public/otel/v1/traces";
    }

    /**
     * @return 规范化后的 Langfuse Web 基址（localhost → 127.0.0.1）
     */
    public String apiBaseUrl() {
        String base = StringUtils.hasText(host) ? host : "http://127.0.0.1:3000";
        if (base.contains("://localhost")) {
            base = base.replace("://localhost", "://127.0.0.1");
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

}
