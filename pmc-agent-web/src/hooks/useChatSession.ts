import { useCallback, useRef, useState } from 'react'
import type { Dispatch, MutableRefObject, SetStateAction } from 'react'
import { resumeChat, streamChat } from '../api/chat'
import { listMessages } from '../api/sessions'
import type { ChatMessage, ChatResponse, ChatStatus, TokenUsage, ToolFeedbackSubmit } from '../types/chat'
import { AGENT_TOOL_NAMES } from '../lib/agentTools'
import { parseTokenUsage } from '../lib/tokenUsage'

export function useChatSession(userId: string) {
  const [sessionId, setSessionId] = useState<number | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [status, setStatus] = useState<ChatStatus>('idle')
  const [error, setError] = useState<string | null>(null)
  const abortRef = useRef<AbortController | null>(null)
  const assistantIdRef = useRef<string | null>(null)

  const stop = useCallback(() => {
    abortRef.current?.abort()
    abortRef.current = null
    setStatus((prev) => (prev === 'streaming' ? 'idle' : prev))
  }, [])

  const load = useCallback(
    async (nextSessionId: number | null) => {
      stop()
      setError(null)
      assistantIdRef.current = null
      setSessionId(nextSessionId)
      if (nextSessionId == null) {
        setMessages([])
        setStatus('idle')
        return
      }
      const history = await listMessages(nextSessionId)
      setMessages(promoteAgentToolNarrativesInHistory(history))
      const last = history.at(-1)
      setStatus(last?.messageType === 'tool-confirm' ? 'awaiting_confirm' : 'idle')
    },
    [stop],
  )

  const consume = useCallback(async (stream: AsyncGenerator<ChatResponse>) => {
    for await (const event of stream) {
      applyEvent(event, setMessages, assistantIdRef)
      if (event.interrupted) {
        setStatus('awaiting_confirm')
        return
      }
    }
    assistantIdRef.current = null
    setMessages((prev) =>
      prev.map((item) => (item.streaming ? { ...item, streaming: false } : item)),
    )
    setStatus('idle')
  }, [])

  const send = useCallback(
    async (activeSessionId: number, prompt: string) => {
      setError(null)
      setStatus('streaming')
      assistantIdRef.current = null
      setMessages((prev) => [
        ...prev,
        { id: crypto.randomUUID(), messageType: 'user', content: prompt },
      ])
      const controller = new AbortController()
      abortRef.current = controller
      try {
        await consume(streamChat(activeSessionId, userId, prompt, controller.signal))
      } catch (err) {
        if (controller.signal.aborted) {
          setStatus('idle')
          return
        }
        setError(err instanceof Error ? err.message : '生成失败，请重试')
        setStatus('error')
      } finally {
        abortRef.current = null
      }
    },
    [consume, userId],
  )

  const resume = useCallback(
    async (activeSessionId: number, toolFeedbacks: ToolFeedbackSubmit[]) => {
      setError(null)
      setStatus('streaming')
      assistantIdRef.current = null
      const controller = new AbortController()
      abortRef.current = controller
      try {
        await consume(resumeChat(activeSessionId, userId, toolFeedbacks, controller.signal))
      } catch (err) {
        if (controller.signal.aborted) {
          setStatus('idle')
          return
        }
        setError(err instanceof Error ? err.message : '恢复失败，请重试')
        setStatus('error')
      } finally {
        abortRef.current = null
      }
    },
    [consume, userId],
  )

  return { sessionId, messages, status, error, load, send, resume, stop, setError }
}

function applyEvent(
  event: ChatResponse,
  setMessages: Dispatch<SetStateAction<ChatMessage[]>>,
  assistantIdRef: MutableRefObject<string | null>,
) {
  const payload = event.messageResponse
  const type = payload?.messageType
  const tokenUsage = parseTokenUsage(event.tokenUsage)
  if (event.chunk) {
    upsertAssistantChunk(
      event.chunk,
      tokenUsage,
      setMessages,
      assistantIdRef,
      event.agentName,
      event.node,
    )
  }
  if (type === 'tool-request' || type === 'tool' || type === 'tool-confirm') {
    assistantIdRef.current = null
    setMessages((prev) => [
      ...prev.map((item) => (item.streaming ? { ...item, streaming: false } : item)),
      {
        id: crypto.randomUUID(),
        messageType: type,
        content: payload?.content ?? '',
        payload,
        agentName: event.agentName,
        node: event.node,
        tokenUsage,
      },
    ])
    return
  }
  if (type === 'assistant' && !event.chunk && payload?.content && !assistantIdRef.current) {
    setMessages((prev) => [
      ...prev,
      {
        id: crypto.randomUUID(),
        messageType: 'assistant',
        content: payload.content ?? '',
        payload,
        agentName: event.agentName,
        node: event.node,
        tokenUsage,
      },
    ])
    return
  }
  if (tokenUsage && !event.chunk) {
    applyUsageToAssistant(tokenUsage, setMessages, assistantIdRef)
  }
}

function upsertAssistantChunk(
  chunk: string,
  tokenUsage: TokenUsage | undefined,
  setMessages: Dispatch<SetStateAction<ChatMessage[]>>,
  assistantIdRef: MutableRefObject<string | null>,
  agentName?: string,
  node?: string,
) {
  const isSubNarrative = node === '_SUB_AGENT_NARRATIVE_'
  const currentId = assistantIdRef.current

  if (isSubNarrative) {
    // 子 Agent 终答独立成条，不与 Supervisor 流式气泡合并
    setMessages((prev) => {
      const finalized = prev.map((item) =>
        item.streaming && item.node !== '_SUB_AGENT_NARRATIVE_'
          ? { ...item, streaming: false }
          : item,
      )
      const last = finalized[finalized.length - 1]
      if (
        last?.messageType === 'assistant' &&
        last.node === '_SUB_AGENT_NARRATIVE_' &&
        last.agentName === agentName &&
        last.streaming
      ) {
        assistantIdRef.current = last.id
        return finalized.map((item) =>
          item.id === last.id
            ? {
                ...item,
                content: mergeChunk(item.content, chunk),
                streaming: true,
                tokenUsage: tokenUsage ?? item.tokenUsage,
              }
            : item,
        )
      }
      const id = crypto.randomUUID()
      assistantIdRef.current = id
      return [
        ...finalized,
        {
          id,
          messageType: 'assistant' as const,
          content: chunk,
          agentName,
          node,
          streaming: true,
          tokenUsage,
        },
      ]
    })
    return
  }

  // Supervisor 流式：若当前挂着子 Agent 气泡则切断
  if (currentId) {
    setMessages((prev) => {
      const current = prev.find((item) => item.id === currentId)
      if (current?.node === '_SUB_AGENT_NARRATIVE_') {
        assistantIdRef.current = null
      }
      return prev
    })
  }

  const activeId = assistantIdRef.current
  if (!activeId) {
    const id = crypto.randomUUID()
    assistantIdRef.current = id
    setMessages((prev) => [
      ...prev.map((item) => (item.streaming ? { ...item, streaming: false } : item)),
      {
        id,
        messageType: 'assistant',
        content: chunk,
        agentName,
        node,
        streaming: true,
        tokenUsage,
      },
    ])
    return
  }
  setMessages((prev) =>
    prev.map((item) => {
      if (item.id !== activeId) {
        return item
      }
      return {
        ...item,
        content: mergeChunk(item.content, chunk),
        streaming: true,
        agentName: agentName ?? item.agentName,
        node: node ?? item.node,
        tokenUsage: tokenUsage ?? item.tokenUsage,
      }
    }),
  )
}

function applyUsageToAssistant(
  tokenUsage: TokenUsage,
  setMessages: Dispatch<SetStateAction<ChatMessage[]>>,
  assistantIdRef: MutableRefObject<string | null>,
) {
  const currentId = assistantIdRef.current
  setMessages((prev) => {
    if (currentId) {
      return prev.map((item) => (item.id === currentId ? { ...item, tokenUsage } : item))
    }
    for (let index = prev.length - 1; index >= 0; index -= 1) {
      if (prev[index].messageType === 'assistant') {
        return prev.map((item, cursor) => (cursor === index ? { ...item, tokenUsage } : item))
      }
    }
    return prev
  })
}

/** 后端已对 chunk 保序，SSE 帧为纯增量，直接追加。 */
function mergeChunk(current: string, chunk: string): string {
  return current + chunk
}

/**
 * 历史消息中 AgentTool 可能只把终答写在 tool.responseData，且后面没有 assistant。
 * 加载时提升为助手气泡，并打上子 Agent 归组标记，兼容旧数据。
 */

/**
 * 历史消息中 AgentTool 可能只把终答写在 tool.responseData，且后面没有 assistant。
 * 加载时提升为助手气泡，并打上子 Agent 归组标记，兼容旧数据。
 */
function promoteAgentToolNarrativesInHistory(messages: ChatMessage[]): ChatMessage[] {
  const result: ChatMessage[] = []
  for (let index = 0; index < messages.length; index += 1) {
    const message = messages[index]
    if (message.messageType !== 'tool' || !message.payload?.responses?.length) {
      result.push(message)
      continue
    }
    const narratives: Array<{ agentName: string; text: string }> = []
    const compactedResponses = message.payload.responses.map((item) => {
      if (!item.name || !AGENT_TOOL_NAMES.has(item.name)) {
        return item
      }
      const raw = item.responseData?.trim()
      if (!raw || !isHistoryAgentNarrative(raw)) {
        return item
      }
      narratives.push({ agentName: item.name, text: raw })
      return {
        ...item,
        responseData: JSON.stringify({
          _promoted: true,
          note: '子Agent回复已作为助手消息展示',
          chars: raw.length,
        }),
      }
    })
    result.push({
      ...message,
      payload: { ...message.payload, responses: compactedResponses },
    })
    const next = messages[index + 1]
    const alreadyAssistant =
      next?.messageType === 'assistant' &&
      narratives.some(({ text }) => (next.content ?? '').includes(text.slice(0, 40)))
    if (!alreadyAssistant) {
      for (let narrativeIndex = 0; narrativeIndex < narratives.length; narrativeIndex += 1) {
        const narrative = narratives[narrativeIndex]
        result.push({
          id: `${message.id}-promoted-${narrativeIndex}`,
          messageType: 'assistant',
          content: narrative.text,
          agentName: narrative.agentName,
          node: '_SUB_AGENT_NARRATIVE_',
        })
      }
    } else if (next?.messageType === 'assistant') {
      const matched = narratives.find(({ text }) => (next.content ?? '').includes(text.slice(0, 40)))
      if (matched) {
        next.agentName = next.agentName ?? matched.agentName
        next.node = next.node ?? '_SUB_AGENT_NARRATIVE_'
      }
    }
  }
  return result
}

function isHistoryAgentNarrative(raw: string): boolean {
  if (raw.startsWith('{') || raw.startsWith('[')) {
    try {
      const parsed = JSON.parse(raw) as Record<string, unknown>
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        if (
          'success' in parsed ||
          'data' in parsed ||
          'downloadUrl' in parsed ||
          'option' in parsed ||
          '_promoted' in parsed
        ) {
          return false
        }
      } else {
        return false
      }
    } catch {
      // prose
    }
  }
  return true
}
