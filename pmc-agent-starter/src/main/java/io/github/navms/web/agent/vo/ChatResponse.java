package io.github.navms.web.agent.vo;

import io.github.navms.web.agent.vo.message.MessageResponse;
import lombok.Data;

/**
 * 会话 SSE 响应。
 *
 * @author navms
 */
@Data
public class ChatResponse {

    /**
     * 图节点名。
     */
    private String node;

    /**
     * Agent 名。
     */
    private String agentName;

    /**
     * 当前消息（assistant / user / tool / tool-request / tool-confirm）。
     */
    private MessageResponse messageResponse;

    /**
     * Token 用量。
     */
    private TokenUsageVO tokenUsage;

    /**
     * 流式文本分片。
     */
    private String chunk;

    /**
     * 是否因 HITL 中断。
     */
    private boolean interrupted;

    public ChatResponse(
            String node,
            String agentName,
            MessageResponse messageResponse,
            TokenUsageVO tokenUsage,
            String chunk) {
        this.node = node;
        this.agentName = agentName;
        this.messageResponse = messageResponse;
        this.tokenUsage = tokenUsage;
        this.chunk = chunk;
    }

}
