package io.github.navms.bank.autoconfigure;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * 银企 Mapper 扫描。
 *
 * @author navms
 */
@AutoConfiguration
@MapperScan("io.github.navms.infrastructure.mapper")
public class BankAutoConfiguration {
}
