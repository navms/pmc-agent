import type { ChatMessage } from '../types/chat'
import { AGENT_TOOL_NAMES } from '../lib/agentTools'

export type RenderItem =
  | { kind: 'single'; message: ChatMessage; index: number }
  | {
      kind: 'subAgent'
      agentName: string
      /** 卡片内消息（内部 tool 轨迹 + 子 Agent 自然语言） */
      children: Array<{ message: ChatMessage; index: number }>
      /** Supervisor 压缩后的 AgentTool 结果，收进卡片底部 */
      toolResult?: { message: ChatMessage; index: number }
    }

function toolNames(message: ChatMessage): string[] {
  const fromCalls = message.payload?.toolCalls?.map((item) => item.name).filter(Boolean) as
    | string[]
    | undefined
  const fromResults = message.payload?.responses?.map((item) => item.name).filter(Boolean) as
    | string[]
    | undefined
  return [...(fromCalls ?? []), ...(fromResults ?? [])]
}

function agentToolFromRequest(message: ChatMessage): string | null {
  if (message.messageType !== 'tool-request') {
    return null
  }
  for (const name of toolNames(message)) {
    if (AGENT_TOOL_NAMES.has(name)) {
      return name
    }
  }
  return null
}

function isSubAgentInner(message: ChatMessage, agentName: string): boolean {
  if (message.node === '_SUB_AGENT_TOOL_' || message.node === '_SUB_AGENT_NARRATIVE_') {
    return !message.agentName || message.agentName === agentName
  }
  if (message.agentName === agentName && message.messageType !== 'user') {
    // 流式过程中尚未带 node 的子 Agent 消息
    return message.messageType === 'tool' || message.messageType === 'tool-request' || message.messageType === 'assistant'
  }
  return false
}

function isSupervisorAgentToolResult(message: ChatMessage, agentName: string): boolean {
  if (message.messageType !== 'tool') {
    return false
  }
  if (message.node === '_SUB_AGENT_TOOL_' || message.node === '_SUB_AGENT_NARRATIVE_') {
    return false
  }
  return toolNames(message).includes(agentName)
}

/**
 * 将扁平消息列表按「一次子 Agent 调用」归组，供嵌套卡片渲染。
 */
export function groupMessagesForRender(messages: ChatMessage[]): RenderItem[] {
  const items: RenderItem[] = []
  let index = 0
  while (index < messages.length) {
    const message = messages[index]
    const agentName = agentToolFromRequest(message)
    if (!agentName) {
      items.push({ kind: 'single', message, index })
      index += 1
      continue
    }

    // Supervisor 调用子 Agent 的 tool-request 留在卡片外作入口
    items.push({ kind: 'single', message, index })
    index += 1

    const children: Array<{ message: ChatMessage; index: number }> = []
    while (index < messages.length && isSubAgentInner(messages[index], agentName)) {
      children.push({ message: messages[index], index })
      index += 1
    }

    let toolResult: { message: ChatMessage; index: number } | undefined
    if (index < messages.length && isSupervisorAgentToolResult(messages[index], agentName)) {
      toolResult = { message: messages[index], index }
      index += 1
    }

    if (children.length > 0 || toolResult) {
      items.push({ kind: 'subAgent', agentName, children, toolResult })
    }
  }
  return items
}
