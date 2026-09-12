package io.github.navms.agent;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.agui.adapter.strategy.AguiEventEnricher;
import io.agentscope.core.agui.runtime.AguiRuntimeContextResolver;
import io.github.navms.application.chat.service.AguiPersistEnricher;
import io.github.navms.application.chat.service.ChatSessionAppService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * AG-UI 运行时上下文与落库。
 *
 * @author navms
 */
@Configuration
public class AguiConfig {

    @Bean
    public AguiRuntimeContextResolver aguiRuntimeContextResolver() {
        return request -> {
            String threadId = request.getInput() == null ? null : request.getInput().getThreadId();
            String userId = request.firstHeader("X-User-Id");
            return RuntimeContext.builder()
                    .sessionId(threadId)
                    .userId(StringUtils.hasText(userId) ? userId : "anonymous")
                    .build();
        };
    }

    /**
     * @param chatSessionAppService 会话落库
     * @return 把 AG-UI 事件写入历史
     */
    @Bean
    public AguiEventEnricher aguiPersistEnricher(ChatSessionAppService chatSessionAppService) {
        return new AguiPersistEnricher(chatSessionAppService);
    }
}
