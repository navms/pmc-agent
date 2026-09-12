import type { ChatMessage, TokenUsage } from '../types/chat'

export function parseTokenUsage(value: unknown): TokenUsage | undefined {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return undefined
  }
  const record = value as Record<string, unknown>
  const promptTokens = toCount(record.promptTokens)
  const completionTokens = toCount(record.completionTokens)
  const totalTokens = toCount(record.totalTokens)
  if (promptTokens == null && completionTokens == null && totalTokens == null) {
    return undefined
  }
  return { promptTokens, completionTokens, totalTokens }
}

export function sumUsage(usages: Array<TokenUsage | undefined>): TokenUsage | undefined {
  let promptTokens = 0
  let completionTokens = 0
  let totalTokens = 0
  let any = false
  for (const usage of usages) {
    if (!usage) {
      continue
    }
    any = true
    promptTokens += usage.promptTokens ?? 0
    completionTokens += usage.completionTokens ?? 0
    totalTokens += usage.totalTokens ?? (usage.promptTokens ?? 0) + (usage.completionTokens ?? 0)
  }
  if (!any) {
    return undefined
  }
  return { promptTokens, completionTokens, totalTokens }
}

export function sessionUsage(messages: ChatMessage[]): TokenUsage | undefined {
  return sumUsage(messages.map((item) => item.tokenUsage))
}

export function usageForTurn(messages: ChatMessage[], index: number): TokenUsage | undefined {
  const start = findTurnStart(messages, index)
  const end = findTurnEnd(messages, index)
  return sumUsage(messages.slice(start, end).map((item) => item.tokenUsage))
}

export function isLastAssistantOfTurn(messages: ChatMessage[], index: number): boolean {
  if (messages[index]?.messageType !== 'assistant') {
    return false
  }
  const end = findTurnEnd(messages, index)
  for (let cursor = end - 1; cursor > index; cursor -= 1) {
    if (messages[cursor].messageType === 'assistant') {
      return false
    }
  }
  return true
}

export function formatTurnUsage(usage: TokenUsage): string {
  return `本轮 Token  入 ${formatCount(usage.promptTokens)} · 出 ${formatCount(usage.completionTokens)} · 共 ${formatCount(resolvedTotal(usage))}`
}

export function formatSessionUsage(usage: TokenUsage): string {
  return `本会话 ${formatCount(resolvedTotal(usage))} tokens`
}

function findTurnStart(messages: ChatMessage[], index: number): number {
  for (let cursor = index; cursor >= 0; cursor -= 1) {
    if (messages[cursor].messageType === 'user') {
      return cursor + 1
    }
  }
  return 0
}

function findTurnEnd(messages: ChatMessage[], index: number): number {
  for (let cursor = index + 1; cursor < messages.length; cursor += 1) {
    if (messages[cursor].messageType === 'user') {
      return cursor
    }
  }
  return messages.length
}

function resolvedTotal(usage: TokenUsage): number {
  if (usage.totalTokens != null) {
    return usage.totalTokens
  }
  return (usage.promptTokens ?? 0) + (usage.completionTokens ?? 0)
}

function formatCount(value: number | undefined): string {
  return (value ?? 0).toLocaleString('zh-CN')
}

function toCount(value: unknown): number | undefined {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }
  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : undefined
  }
  return undefined
}
