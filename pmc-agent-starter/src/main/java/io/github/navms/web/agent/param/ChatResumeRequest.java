package io.github.navms.web.agent.param;

import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;

import java.util.List;

/**
 * 恢复被中断的 Agent 执行。
 *
 * @param sessionId     会话 ID
 * @param userId        用户标识
 * @param toolFeedbacks 人工对工具调用的确认结果
 * @author navms
 */
public record ChatResumeRequest(
        Long sessionId,
        String userId,
        List<InterruptionMetadata.ToolFeedback> toolFeedbacks
) {
}
