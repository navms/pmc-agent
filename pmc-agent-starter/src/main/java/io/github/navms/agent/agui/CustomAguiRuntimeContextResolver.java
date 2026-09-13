package io.github.navms.agent.agui;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.agui.runtime.AguiRuntimeContextRequest;
import io.agentscope.core.agui.runtime.AguiRuntimeContextResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 自定义 AguiRuntimeContextResolver
 *
 * @author navms
 */
@Slf4j
@Component
public class CustomAguiRuntimeContextResolver implements AguiRuntimeContextResolver {

    @Override
    public RuntimeContext resolve(AguiRuntimeContextRequest<?> request) {
        String threadId = request.getInput() == null ? null : request.getInput().getThreadId();
        String userId = request.firstHeader("X-User-Id");
        return RuntimeContext.builder()
                .sessionId(threadId)
                .userId(StringUtils.hasText(userId) ? userId : "anonymous")
                .build();
    }

}
