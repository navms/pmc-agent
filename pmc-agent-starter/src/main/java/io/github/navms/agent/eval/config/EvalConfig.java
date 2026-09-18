package io.github.navms.agent.eval.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Eval Harness 配置。
 *
 * @author navms
 */
@Configuration
@EnableConfigurationProperties(EvalProperties.class)
public class EvalConfig {
}
