package io.github.navms.agent.clarify;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntentClarifyParserTest {

    @Test
    void rewriteReadyIncludesRouteAndOriginal() {
        IntentClarifyResult result = new IntentClarifyResult(
                "ready",
                "summarize",
                "汇总账号 6013821000000008000212 在 2029-01-23 的交易金额合计",
                "",
                List.of());
        String text = IntentClarifyParser.rewriteUserMessage("那 6013821000000008000212 这个账号呢？", result);
        assertTrue(text.contains("【已澄清任务】"));
        assertTrue(text.contains("summarize_bank"));
        assertTrue(text.contains("6013821000000008000212"));
        assertTrue(text.contains("2029-01-23"));
        assertTrue(text.contains("那 6013821000000008000212 这个账号呢？"));
    }

    @Test
    void rewriteClarifyForbidsTools() {
        IntentClarifyResult result = new IntentClarifyResult(
                "need_clarify",
                "summarize",
                "",
                "请问要查哪一天的交易金额？",
                List.of("date"));
        String text = IntentClarifyParser.rewriteUserMessage("交易金额有多少", result);
        assertTrue(text.contains("【意图澄清】"));
        assertTrue(text.contains("不要 spawn"));
        assertTrue(text.contains("请问要查哪一天的交易金额？"));
        assertTrue(text.contains("交易金额有多少"));
    }

    @Test
    void historyJoin() {
        String joined = IntentClarifyParser.HistoryTurn.join(List.of(
                new IntentClarifyParser.HistoryTurn("user", "6013821000000008008 在2029-01-23 的交易金额有多少"),
                new IntentClarifyParser.HistoryTurn("assistant", "合计 187 万")));
        assertTrue(joined.contains("user: 6013821000000008008"));
        assertTrue(joined.contains("assistant: 合计 187 万"));
        assertEquals("(无)", IntentClarifyParser.HistoryTurn.join(List.of()));
        assertFalse(IntentClarifyParser.routeOf("query").isBlank());
        assertEquals("summarize_bank", IntentClarifyParser.routeOf("summarize"));
    }

    @Test
    void validRejectsUnknownStatus() {
        assertFalse(new IntentClarifyResult("maybe", "query", "", "", List.of()).valid());
        assertTrue(new IntentClarifyResult("ready", "summarize", "x", "", List.of()).valid());
    }
}
