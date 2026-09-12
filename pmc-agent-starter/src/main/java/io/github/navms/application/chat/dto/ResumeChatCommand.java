package io.github.navms.application.chat.dto;

import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;

import java.util.List;

/**
 * 恢复被中断的 Agent 执行。
 *
 * @param sessionId     会话 ID
 * @param userId        用户标识
 * @param toolFeedbacks 工具确认结果
 * @author navms
 */
public record ResumeChatCommand(
        Long sessionId,
        String userId,
        List<InterruptionMetadata.ToolFeedback> toolFeedbacks
) {
}
