package io.github.navms.agent.eval;

import org.apache.commons.collections4.CollectionUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 一次 Trial 从 Transcript 抽出的可观察事实。
 *
 * @param subAgents     业务子 Agent
 * @param writeTools    父 Agent 写工具
 * @param interrupted   是否 HITL interrupt
 * @param interruptIds  resume 用 id
 * @param clarified     是否出现意图澄清 start 或改写块
 * @param assistantText 助手可见正文
 * @param harnessError  Harness/环境错误，空表示无
 * @param latencyMs     本 Trial 耗时
 * @author navms
 */
public record TranscriptObservation(
        Set<String> subAgents,
        Set<String> writeTools,
        boolean interrupted,
        List<String> interruptIds,
        boolean clarified,
        String assistantText,
        String harnessError,
        long latencyMs) {

    /**
     * @return 用于集合评测的实际工具（子 Agent + 写工具）
     */
    public Set<String> actualTools() {
        LinkedHashSet<String> tools = new LinkedHashSet<>();
        if (subAgents != null) {
            tools.addAll(subAgents);
        }
        if (writeTools != null) {
            tools.addAll(writeTools);
        }
        return tools;
    }

    /**
     * @return 是否 Harness 失败
     */
    public boolean failed() {
        return harnessError != null && !harnessError.isBlank();
    }

    /**
     * @return 空观察
     */
    public static TranscriptObservation empty() {
        return new TranscriptObservation(Set.of(), Set.of(), false, List.of(), false, "", null, 0L);
    }

    /**
     * @return 空观察
     */
    public static TranscriptObservation empty(String error) {
        return new TranscriptObservation(Set.of(), Set.of(), false, List.of(), false, "", error, 0L);
    }

    /**
     * @param observations 各轮观察
     * @param latencyMs    总耗时
     * @return 合并结果
     */
    public static TranscriptObservation merge(List<TranscriptObservation> observations, long latencyMs) {
        if (CollectionUtils.isEmpty(observations)) {
            return empty("empty transcript");
        }

        Set<String> agents = new LinkedHashSet<>();
        Set<String> writes = new LinkedHashSet<>();
        boolean interrupted = false;
        List<String> interruptIds = List.of();
        boolean clarified = false;
        StringBuilder text = new StringBuilder();
        String harnessError = null;
        for (TranscriptObservation observation : observations) {
            if (observation == null) {
                continue;
            }
            agents.addAll(observation.subAgents());
            writes.addAll(observation.writeTools());
            interrupted = interrupted || observation.interrupted();
            if (observation.interrupted() && CollectionUtils.isNotEmpty(observation.interruptIds())) {
                interruptIds = observation.interruptIds();
            }
            clarified = clarified || observation.clarified();
            if (observation.assistantText() != null && !observation.assistantText().isBlank()) {
                if (!text.isEmpty()) {
                    text.append('\n');
                }
                text.append(observation.assistantText());
            }
            if (harnessError == null && observation.failed()) {
                harnessError = observation.harnessError();
            }
        }
        return new TranscriptObservation(agents, writes, interrupted, interruptIds, clarified, text.toString(), harnessError, latencyMs);
    }

}
