package io.github.navms.agent.hitl;

import io.agentscope.core.message.ToolUseBlock;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 缓存父 Agent 写工具权限 ASK 的 pending 确认，供 AG-UI resume 转 ConfirmResult。
 * <p>
 * TODO 分布式场景下有问题
 *
 * @author navms
 */
@Component
public class WritePermissionHitlStore {

    private final ConcurrentMap<String, PendingConfirm> byThreadId = new ConcurrentHashMap<>();

    /**
     * @param threadId  AG-UI threadId
     * @param replyId   确认请求 replyId
     * @param toolCalls 待确认工具
     */
    public void save(String threadId, String replyId, List<ToolUseBlock> toolCalls) {
        if (!StringUtils.hasText(threadId) || CollectionUtils.isEmpty(toolCalls)) {
            return;
        }
        byThreadId.put(threadId, new PendingConfirm(replyId, List.copyOf(toolCalls)));
    }

    /**
     * @param threadId AG-UI threadId
     * @return pending，不存在则 null
     */
    public PendingConfirm peek(String threadId) {
        if (!StringUtils.hasText(threadId)) {
            return null;
        }
        return byThreadId.get(threadId);
    }

    /**
     * @param threadId AG-UI threadId
     * @return pending，并移除
     */
    public PendingConfirm take(String threadId) {
        if (!StringUtils.hasText(threadId)) {
            return null;
        }
        return byThreadId.remove(threadId);
    }

    /**
     * @param replyId   请求 replyId
     * @param toolCalls 工具调用
     */
    public record PendingConfirm(String replyId, List<ToolUseBlock> toolCalls) {
    }
}
