package io.github.navms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Agent 启动入口。
 *
 * @author navms
 */
@SpringBootApplication(scanBasePackages = {
        "io.github.navms.application",
        "io.github.navms.agent",
        "io.github.navms.domain.chat",
        "io.github.navms.infrastructure",
        "io.github.navms.tool",
        "io.github.navms.web"
})
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
