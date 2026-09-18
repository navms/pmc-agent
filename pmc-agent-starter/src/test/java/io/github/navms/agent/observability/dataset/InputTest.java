package io.github.navms.agent.observability.dataset;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.navms.domain.chat.entity.ChatMessage;
import io.github.navms.domain.chat.entity.ChatSession;
import io.github.navms.domain.chat.enums.ChatMessageType;
import io.github.navms.domain.chat.valueobj.SessionTitle;
import io.github.navms.domain.chat.valueobj.UserId;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapsBaseCaseTranscript() throws Exception {
        String json = """
                {
                  "title": "6013821000000008008 在2029-01-23 的交易金额有多少",
                  "sessionId": 32,
                  "messages": [
                    {
                      "id": "f614d344-ecab-4b5f-9b05-b35bd1b857af",
                      "role": "user",
                      "content": "6013821000000008008 在2029-01-23 的交易金额有多少",
                      "metadata": { "seq": 0, "createdAt": "2026-09-18T11:31:41" }
                    },
                    {
                      "id": "8ea9dd4ec5514766bd41da50b42fb117-reasoning",
                      "role": "reasoning",
                      "content": "应使用 summarize_bank",
                      "metadata": { "seq": 1, "agentName": "general_chat", "createdAt": "2026-09-18T11:31:52" }
                    },
                    {
                      "id": "6ab2ad8e-c753-47c2-9b38-47040ed592fb",
                      "name": "summarize_bank",
                      "role": "assistant",
                      "metadata": { "seq": 3, "createdAt": "2026-09-18T11:32:13" },
                      "toolCalls": [
                        {
                          "id": "call_31ee37a2f7be4284a1986b0d",
                          "type": "function",
                          "function": { "name": "summarizeTradeDetails", "arguments": "" }
                        }
                      ]
                    },
                    {
                      "id": "7a9c459f-f551-4e44-9d6f-c44db098515b",
                      "role": "tool",
                      "content": "",
                      "metadata": {
                        "seq": 4,
                        "toolName": "summarizeTradeDetails",
                        "agentName": "summarize_bank",
                        "createdAt": "2026-09-18T11:32:13"
                      },
                      "toolCallId": "call_31ee37a2f7be4284a1986b0d"
                    },
                    {
                      "id": "dca0ca04-4480-4266-b6ef-a48080fdb741",
                      "name": "summarize_bank",
                      "role": "assistant",
                      "content": "借方 46 笔",
                      "metadata": {
                        "seq": 6,
                        "createdAt": "2026-09-18T11:32:21",
                        "tokenUsage": {
                          "totalTokens": 6215,
                          "promptTokens": 5959,
                          "completionTokens": 256
                        }
                      }
                    },
                    {
                      "id": "239de913-b0ee-4f34-a7ba-0f9927703f8d",
                      "role": "user",
                      "content": "还有其他账号吗",
                      "metadata": { "seq": 10, "createdAt": "2026-09-18T11:33:16" }
                    }
                  ]
                }
                """;

        Input input = mapper.readValue(json, Input.class);
        assertEquals("6013821000000008008 在2029-01-23 的交易金额有多少", input.title());
        assertEquals(32L, input.sessionId());
        assertEquals(6, input.messages().size());
        assertEquals("user", input.messages().getFirst().role());
        assertEquals(0, input.messages().getFirst().metadata().seq());
        Input.ToolCall toolCall = input.messages().get(2).toolCalls().getFirst();
        assertEquals("summarizeTradeDetails", toolCall.function().name());
        assertEquals("", toolCall.function().arguments());
        assertEquals("call_31ee37a2f7be4284a1986b0d", input.messages().get(3).toolCallId());
        assertEquals("summarizeTradeDetails", input.messages().get(3).metadata().toolName());
        assertEquals(6215, input.messages().get(4).metadata().tokenUsage().totalTokens());
        assertEquals(2, input.userTurns().size());
        assertEquals("还有其他账号吗", input.userTurns().get(1).content());
        assertTrue(input.messages().getFirst().userTurn());
        assertNotNull(input.messages().get(1).metadata().agentName());
    }

    @Test
    void fromSessionSnapshot() {
        ChatSession session = new ChatSession.Builder(UserId.of("user-1"))
                .id(9L)
                .title(new SessionTitle("查流水"))
                .build();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", "m-user");
        payload.put("role", "user");
        payload.put("content", "hello");
        ChatMessage row = new ChatMessage.Builder(9L, ChatMessageType.USER).message(payload).seq(0).build();

        Input input = Input.from(session, List.of(row));
        assertEquals("查流水", input.title());
        assertEquals(9L, input.sessionId());
        assertEquals(1, input.userTurns().size());
        assertEquals("hello", input.userTurns().getFirst().content());
        assertEquals(0, input.messages().getFirst().metadata().seq());
    }
}
