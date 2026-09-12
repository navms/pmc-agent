package io.github.navms.web.agent.param;

/**
 * 流式对话请求。
 *
 * @param sessionId 会话 ID
 * @param userId    用户标识
 * @param prompt    用户输入
 * @author navms
 */
public record ChatRequest(Long sessionId, String userId, String prompt) {
}
