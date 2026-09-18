package io.github.navms.agent.prompt;

import com.langfuse.client.LangfuseClient;
import com.langfuse.client.core.LangfuseClientApiException;
import com.langfuse.client.resources.prompts.requests.GetPromptRequest;
import com.langfuse.client.resources.prompts.types.CreatePromptRequest;
import com.langfuse.client.resources.prompts.types.CreateTextPromptRequest;
import com.langfuse.client.resources.prompts.types.CreateTextPromptType;
import com.langfuse.client.resources.prompts.types.Prompt;
import com.langfuse.client.resources.prompts.types.TextPrompt;
import io.github.navms.agent.observability.LangfuseProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 基于官方 {@link LangfuseClient} 的 Prompt Public API。
 *
 * @author navms
 */
@Slf4j
@Component
public class LangfusePromptClient {

    private final LangfuseProperties properties;
    private final ObjectProvider<LangfuseClient> langfuseClient;

    public LangfusePromptClient(LangfuseProperties properties, ObjectProvider<LangfuseClient> langfuseClient) {
        this.properties = properties;
        this.langfuseClient = langfuseClient;
    }

    /**
     * @param name  prompt 名，可含 /
     * @param label 如 production
     * @return 远端版本
     */
    public Optional<LangfusePromptSnapshot> fetch(String name, String label) {
        LangfuseClient client = clientOrNull();
        if (client == null) {
            return Optional.empty();
        }
        try {
            Prompt prompt = client.prompts()
                    .get(name, GetPromptRequest.builder().label(label).build());
            return promptOf(name, prompt);
        } catch (LangfuseClientApiException e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            log.warn("Langfuse GET prompt {} failed: {} {}", name, e.statusCode(), e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Langfuse GET prompt {} failed: {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 创建新 version；已存在同名时追加 version。
     *
     * @param name     prompt 名
     * @param template 正文
     * @param label    打上的 label
     * @return 创建结果
     */
    public Optional<LangfusePromptSnapshot> create(String name, String template, String label) {
        LangfuseClient client = clientOrNull();
        if (client == null) {
            return Optional.empty();
        }
        try {
            CreateTextPromptRequest textRequest = CreateTextPromptRequest.builder()
                    .name(name)
                    .prompt(template)
                    .type(CreateTextPromptType.TEXT)
                    .labels(List.of(label))
                    .commitMessage("seeded by pmc-agent")
                    .build();
            return promptOf(name, client.prompts().create(CreatePromptRequest.of(textRequest)));
        } catch (Exception e) {
            log.warn("Langfuse POST prompt {} failed: {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    private LangfuseClient clientOrNull() {
        if (!properties.isLangfuseEnable()) {
            return null;
        }
        return langfuseClient.getIfAvailable();
    }

    private static Optional<LangfusePromptSnapshot> promptOf(String name, Prompt prompt) {
        if (prompt == null) {
            return Optional.empty();
        }
        Optional<TextPrompt> text = prompt.getText();
        if (text.isEmpty()) {
            log.warn("Langfuse prompt {} is not text type", name);
            return Optional.empty();
        }
        TextPrompt tp = text.get();
        String template = tp.getPrompt();
        if (!StringUtils.hasText(template)) {
            return Optional.empty();
        }
        return Optional.of(new LangfusePromptSnapshot(name, tp.getVersion(), template, false));
    }

}
