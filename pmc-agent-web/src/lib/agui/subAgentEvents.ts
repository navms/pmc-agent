import type { AssistantMessage, CustomEvent, Message, RawEvent, ReasoningMessage, ToolMessage } from '@ag-ui/client'
import type { TokenUsage } from '../../types/chat'
import { parseTokenUsage } from '../tokenUsage'
import { upsertMessage } from './message'

export interface SubAgentScratch {
  subText: Map<string, string>
  subReasoning: Map<string, string>
  toolArg: Map<string, string>
  toolName: Map<string, string>
}

export function createSubAgentScratch(): SubAgentScratch {
  return {
    subText: new Map(),
    subReasoning: new Map(),
    toolArg: new Map(),
    toolName: new Map(),
  }
}

export function applyCustomEvent(
  event: CustomEvent,
  messages: Message[],
  scratch: SubAgentScratch,
): Message[] {
  const name = event.name
  const value = asRecord(event.value)
  const agentName = agentNameFromSource(typeof value.source === 'string' ? value.source : undefined)
  if (name === 'token_usage') {
    const usage = parseTokenUsage(value.delta) ?? parseTokenUsage(value)
    if (!usage) {
      return messages
    }
    return applyUsage(messages, usage)
  }
  if (name === 'subagent.text') {
    const delta = typeof value.delta === 'string' ? value.delta : ''
    if (!delta) {
      return messages
    }
    return appendOrPatch(messages, scratch.subText, agentName, 'assistant', delta)
  }
  if (name === 'subagent.thinking') {
    const delta = typeof value.delta === 'string' ? value.delta : ''
    if (!delta) {
      return messages
    }
    return appendOrPatch(messages, scratch.subReasoning, agentName, 'reasoning', delta)
  }
  if (name === 'subagent.tool_call') {
    const toolCallId = String(value.toolCallId ?? '')
    const toolName = String(value.toolName ?? '')
    const eventType = String(value.type ?? '')
    if (eventType === 'TOOL_CALL_START') {
      scratch.toolName.set(toolCallId, toolName)
      scratch.toolArg.set(toolCallId, '')
      return closeStream(messages, scratch.subText, agentName)
    }
    if (eventType === 'TOOL_CALL_END') {
      const args = scratch.toolArg.get(toolCallId) ?? ''
      scratch.toolArg.delete(toolCallId)
      scratch.toolName.set(toolCallId, toolName)
      const next = closeStream(messages, scratch.subText, agentName)
      return upsertMessage(next, assistantToolCall(agentName, toolCallId, toolName, args))
    }
    return messages
  }
  if (name === 'subagent.tool_result') {
    const toolCallId = String(value.toolCallId ?? '')
    const toolName = String(value.toolName ?? scratch.toolName.get(toolCallId) ?? '')
    const data = scratch.toolArg.get(`result:${toolCallId}`) ?? ''
    scratch.toolArg.delete(`result:${toolCallId}`)
    return upsertMessage(messages, toolResult(agentName, toolCallId, toolName, data))
  }
  if (name === 'subagent.lifecycle' && value.type === 'AGENT_END') {
    let next = closeStream(messages, scratch.subReasoning, agentName)
    next = closeStream(next, scratch.subText, agentName)
    return next
  }
  return messages
}

export function applyRawEvent(event: RawEvent, messages: Message[], scratch: SubAgentScratch): Message[] {
  const raw = asRecord(event.event)
  const agentName = agentNameFromSource(typeof event.source === 'string' ? event.source : undefined)
  const rawType = String(raw.type ?? '')
  const toolCallId = String(raw.toolCallId ?? '')
  const delta = String(raw.delta ?? '')
  if (rawType === 'TOOL_CALL_DELTA' && toolCallId && delta) {
    scratch.toolArg.set(toolCallId, (scratch.toolArg.get(toolCallId) ?? '') + delta)
    return messages
  }
  if (rawType === 'TOOL_RESULT_TEXT_DELTA' && toolCallId && delta) {
    scratch.toolArg.set(`result:${toolCallId}`, (scratch.toolArg.get(`result:${toolCallId}`) ?? '') + delta)
    return messages
  }
  if (rawType === 'TEXT_BLOCK_DELTA' && delta) {
    return appendOrPatch(messages, scratch.subText, agentName, 'assistant', delta)
  }
  return messages
}

function appendOrPatch(
  messages: Message[],
  ids: Map<string, string>,
  agentName: string,
  role: 'assistant' | 'reasoning',
  delta: string,
): Message[] {
  const existing = ids.get(agentName)
  if (!existing) {
    const id = crypto.randomUUID()
    ids.set(agentName, id)
    if (role === 'reasoning') {
      const message: ReasoningMessage = {
        id,
        role: 'reasoning',
        content: delta,
        metadata: { agentName },
      }
      return upsertMessage(messages, message)
    }
    const message: AssistantMessage = { id, role: 'assistant', content: delta, name: agentName }
    return upsertMessage(messages, message)
  }
  return messages.map((item) => {
    if (item.id !== existing) {
      return item
    }
    if (item.role === 'assistant' || item.role === 'reasoning') {
      return { ...item, content: item.content + delta }
    }
    return item
  })
}

function closeStream(messages: Message[], ids: Map<string, string>, agentName: string): Message[] {
  ids.delete(agentName)
  return messages
}

function assistantToolCall(agentName: string, toolCallId: string, toolName: string, args: string): AssistantMessage {
  return {
    id: crypto.randomUUID(),
    role: 'assistant',
    name: agentName,
    toolCalls: [
      {
        id: toolCallId,
        type: 'function',
        function: { name: toolName, arguments: args },
      },
    ],
  }
}

function toolResult(agentName: string, toolCallId: string, toolName: string, data: string): ToolMessage {
  return {
    id: crypto.randomUUID(),
    role: 'tool',
    toolCallId,
    content: data,
    metadata: { toolName, agentName },
  }
}

function applyUsage(messages: Message[], tokenUsage: TokenUsage): Message[] {
  for (let index = messages.length - 1; index >= 0; index -= 1) {
    const item = messages[index]
    if (item.role === 'assistant') {
      return messages.map((message, cursor) =>
        cursor === index
          ? { ...message, metadata: { ...message.metadata, tokenUsage } }
          : message,
      )
    }
  }
  return messages
}

function agentNameFromSource(source?: string | null): string {
  if (!source) {
    return 'general_chat'
  }
  const segment = source.includes('/') ? source.slice(source.lastIndexOf('/') + 1) : source
  if (segment === 'pmc_supervisor') {
    return 'general_chat'
  }
  return segment
}

function asRecord(value: unknown): Record<string, unknown> {
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    return value as Record<string, unknown>
  }
  return {}
}
