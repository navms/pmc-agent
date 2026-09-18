package io.github.navms.agent.clarify;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentStartEvent;
import io.agentscope.core.event.CustomEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.github.navms.agent.clarify.IntentClarifyParser.HistoryTurn;
import io.github.navms.domain.chat.entity.ChatMessage;
import io.github.navms.domain.chat.enums.ChatMessageType;
import io.github.navms.domain.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * 在 pmc_supervisor 进入 ReAct 前改写本轮用户消息，并向前端推送澄清 loading 事件。
 *
 * @author navms
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentClarifyMiddleware implements MiddlewareBase {

    static final String SUPERVISOR_NAME = "pmc_supervisor";

    static final String EVENT_START = "intent_clarify.start";

    static final String EVENT_END = "intent_clarify.end";

    private static final int HISTORY_LIMIT = 20;

    private final IntentClarifyService intentClarifyService;

    private final ChatMessageRepository chatMessageRepository;

    @Override
    public Flux<AgentEvent> onAgent(
            Agent agent,
            RuntimeContext context,
            AgentInput input,
            Function<AgentInput, Flux<AgentEvent>> next) {
        if (agent == null || !SUPERVISOR_NAME.equals(agent.getName()) || input == null || CollectionUtils.isEmpty(input.msgs())) {
            return next.apply(input);
        }
        Msg lastUser = lastUserMessage(input.msgs());
        if (lastUser == null || !StringUtils.hasText(lastUser.getTextContent())) {
            return next.apply(input);
        }
        String original = lastUser.getTextContent().trim();
        if (isHitlResumeText(original)) {
            return next.apply(input);
        }
        String replyId = UUID.randomUUID().toString().replace("-", "");
        return Flux.concat(
                Flux.just(new AgentStartEvent(null, replyId, agent.getName())),
                Flux.just(new CustomEvent(EVENT_START, Map.of())),
                Mono.fromCallable(() -> clarifyInput(input, lastUser, original, context))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMapMany(forwarded -> Flux.concat(
                                Flux.just(new CustomEvent(EVENT_END, Map.of())),
                                next.apply(forwarded).filter(e -> !(e instanceof AgentStartEvent)))));
    }

    private AgentInput clarifyInput(AgentInput input, Msg lastUser, String original, RuntimeContext context) {
        List<HistoryTurn> history = loadHistory(context == null ? null : context.getSessionId(), original);
        Optional<IntentClarifyResult> classified = intentClarifyService.classify(original, history, context);
        if (classified.isEmpty()) {
            return input;
        }
        IntentClarifyResult result = classified.get();
        String rewritten = IntentClarifyParser.rewriteUserMessage(original, result);
        log.info(
                "Intent clarify session={} status={} intent={}",
                context == null ? null : context.getSessionId(),
                result.status(),
                result.intent());
        return replaceLastUser(input, lastUser, rewritten);
    }

    static Msg lastUserMessage(List<Msg> msgs) {
        for (int i = msgs.size() - 1; i >= 0; i--) {
            Msg msg = msgs.get(i);
            if (msg != null && msg.getRole() == MsgRole.USER) {
                return msg;
            }
        }
        return null;
    }

    static boolean isHitlResumeText(String text) {
        String value = text.trim().toLowerCase(Locale.ROOT);
        return "approved".equals(value) || "denied".equals(value);
    }

    private List<HistoryTurn> loadHistory(String sessionId, String currentText) {
        Long id = parseSessionId(sessionId);
        if (id == null) {
            return List.of();
        }
        try {
            List<ChatMessage> rows = chatMessageRepository.listBySessionId(id);
            List<HistoryTurn> turns = new ArrayList<>();
            for (ChatMessage row : rows) {
                if (row.getRole() != ChatMessageType.USER && row.getRole() != ChatMessageType.ASSISTANT) {
                    continue;
                }
                String text = textOf(row);
                if (!StringUtils.hasText(text)) {
                    continue;
                }
                turns.add(new HistoryTurn(row.getRole().getCode(), text.trim()));
            }
            if (!turns.isEmpty()) {
                HistoryTurn last = turns.getLast();
                if ("user".equals(last.role()) && currentText.equals(last.text())) {
                    turns.removeLast();
                }
            }
            if (turns.size() > HISTORY_LIMIT) {
                return List.copyOf(turns.subList(turns.size() - HISTORY_LIMIT, turns.size()));
            }
            return turns;
        } catch (Exception e) {
            log.warn("Intent clarify history load failed session={}: {}", sessionId, e.getMessage());
            return List.of();
        }
    }

    private static String textOf(ChatMessage row) {
        Map<String, Object> message = row.getMessage();
        if (message == null) {
            return "";
        }
        Object content = message.get("content");
        return content == null ? "" : String.valueOf(content);
    }

    private static Long parseSessionId(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        try {
            return Long.valueOf(sessionId.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static AgentInput replaceLastUser(AgentInput input, Msg lastUser, String rewritten) {
        List<Msg> msgs = new ArrayList<>(input.msgs());
        for (int i = msgs.size() - 1; i >= 0; i--) {
            if (msgs.get(i) == lastUser) {
                msgs.set(i, Msg.builder()
                        .id(lastUser.getId())
                        .name(lastUser.getName())
                        .role(MsgRole.USER)
                        .textContent(rewritten)
                        .metadata(lastUser.getMetadata())
                        .timestamp(lastUser.getTimestamp())
                        .build());
                break;
            }
        }
        return new AgentInput(msgs);
    }
}
