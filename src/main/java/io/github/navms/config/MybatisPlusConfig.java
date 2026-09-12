package io.github.navms.config;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * MyBatis-Plus 扫描、JSON TypeHandler 与编程式事务。
 *
 * @author navms
 */
@Configuration
@MapperScan({"io.github.navms.infrastructure.chat.mapper", "io.github.navms.infrastructure.bank.mapper"})
public class MybatisPlusConfig {

    public MybatisPlusConfig(ObjectMapper objectMapper) {
        JacksonTypeHandler.setObjectMapper(objectMapper);
    }

    /**
     * @param transactionManager 事务管理器
     * @return 编程式事务模板
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
