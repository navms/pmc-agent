package io.github.navms.web.agent;

import io.github.navms.application.chat.service.ChatAgentAppService;
import io.github.navms.domain.chat.exception.BusinessException;
import io.github.navms.web.agent.converter.ChatWebConverter;
import io.github.navms.web.agent.param.ChatRequest;
import io.github.navms.web.agent.param.ChatResumeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

/**
 * Supervisor 流式执行与中断恢复。
 *
 * @author navms
 */
@RestController
public class ChatController {

    private final ChatAgentAppService chatAgentAppService;

    public ChatController(ChatAgentAppService chatAgentAppService) {
        this.chatAgentAppService = chatAgentAppService;
    }

    /**
     * @param request 流式请求
     * @return SSE
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestBody ChatRequest request) {
        return chatAgentAppService.stream(ChatWebConverter.INSTANCE.toStreamChatCommand(request))
                .onErrorMap(this::mapStreamError);
    }

    /**
     * @param request 恢复请求
     * @return SSE
     */
    @PostMapping(value = "/resume_stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> resumeStream(@RequestBody ChatResumeRequest request) {
        return chatAgentAppService.resume(ChatWebConverter.INSTANCE.toResumeChatCommand(request))
                .onErrorMap(this::mapStreamError);
    }

    private Throwable mapStreamError(Throwable error) {
        if (error instanceof BusinessException businessException) {
            return WebExceptionHandler.toResponseStatus(businessException);
        }
        if (error instanceof ResponseStatusException) {
            return error;
        }
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Agent run failed", error);
    }
}
