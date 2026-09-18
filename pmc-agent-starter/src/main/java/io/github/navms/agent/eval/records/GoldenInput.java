package io.github.navms.agent.eval.records;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.navms.agent.eval.GoldenTurn;

import java.util.ArrayList;
import java.util.List;

/**
 * classpath / Langfuse Golden item 的 input。
 *
 * @param title 说明
 * @param turns 用户轮次
 * @author navms
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoldenInput(String title, List<GoldenTurn> turns) {

    /**
     * @return 非空用户轮次
     */
    public List<GoldenTurn> replayTurns() {
        if (turns == null || turns.isEmpty()) {
            return List.of();
        }
        List<GoldenTurn> filtered = new ArrayList<>();
        for (GoldenTurn turn : turns) {
            if (turn != null && turn.content() != null && !turn.content().isBlank()) {
                filtered.add(turn);
            }
        }
        return List.copyOf(filtered);
    }
}
