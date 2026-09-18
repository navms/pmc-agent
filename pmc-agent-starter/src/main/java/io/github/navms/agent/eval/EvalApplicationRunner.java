package io.github.navms.agent.eval;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 仅在 pmc.eval.enabled=true 时跑批并退出。
 *
 * @author navms
 */
@Order
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "pmc.eval", name = "enabled", havingValue = "true")
public class EvalApplicationRunner implements ApplicationRunner {

    private final EvalHarness evalHarness;

    private final ConfigurableApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        int code;
        try {
            code = evalHarness.execute();
        } catch (Exception e) {
            log.error("Eval harness failed", e);
            code = 2;
        }
        final int exitCode = code;
        int exit = SpringApplication.exit(context, () -> exitCode);
        System.exit(exit);
    }

}
