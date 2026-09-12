package io.github.navms;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * bank-core 集成测试启动类。
 *
 * @author navms
 */
@SpringBootApplication(scanBasePackages = {
        "io.github.navms.application.bank",
        "io.github.navms.infrastructure",
        "io.github.navms.bank.autoconfigure"
})
public class BankCoreTestApplication {
}
