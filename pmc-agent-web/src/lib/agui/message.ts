import type { ActivityMessage, AssistantMessage, Message, ToolMessage } from '@ag-ui/client'

export function agentNameOf(message: Message): string | undefined {
  if ('name' in message && typeof message.name === 'string' && message.name) {
    return message.name
  }
  const fromMeta = message.metadata?.agentName
  return typeof fromMeta === 'string' && fromMeta ? fromMeta : undefined
}

export function textContent(message: Message): string {
  if (!('content' in message) || message.content == null) {
    return ''
  }
  return typeof message.content === 'string' ? message.content : ''
}

export function withAgentName(message: Message, name = 'general_chat'): Message {
  if (message.role === 'user' || agentNameOf(message)) {
    return message
  }
  if (message.role === 'assistant' || message.role === 'system' || message.role === 'developer') {
    return { ...message, name }
  }
  return { ...message, metadata: { ...message.metadata, agentName: name } }
}

export function isConfirmActivity(message: Message | undefined): message is ActivityMessage {
  return !!message && message.role === 'activity' && message.activityType === 'TOOL_CONFIRM'
}

export function isAssistant(message: Message): message is AssistantMessage {
  return message.role === 'assistant'
}

export function isTool(message: Message): message is ToolMessage {
  return message.role === 'tool'
}

export function upsertMessage(messages: Message[], message: Message): Message[] {
  const index = messages.findIndex((item) => item.id === message.id)
  if (index < 0) {
    return [...messages, message]
  }
  const next = [...messages]
  next[index] = message
  return next
}

export function upsertAll(messages: Message[], incoming: Message[]): Message[] {
  return incoming.reduce(upsertMessage, messages)
}

export function cloneMessages(messages: readonly Message[]): Message[] {
  return structuredClone(messages) as Message[]
}
