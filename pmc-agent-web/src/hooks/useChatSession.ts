import { useCallback, useRef, useState } from 'react'
import type { ActivityMessage, AgentSubscriber, HttpAgent, Interrupt, Message, UserMessage } from '@ag-ui/client'
import { createChatAgent, isAbortError, runAgent } from '../api/chat'
import { listMessages } from '../api/sessions'
import type { ChatStatus } from '../types/chat'
import { cloneMessages, isConfirmActivity, upsertAll, withAgentName } from '../lib/agui/message'
import { applyCustomEvent, applyRawEvent, createSubAgentScratch } from '../lib/agui/subAgentEvents'
import type { ResumeEntry } from '@ag-ui/core'

export function useChatSession(userId: string) {
  const [sessionId, setSessionId] = useState<number | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [status, setStatus] = useState<ChatStatus>('idle')
  const [error, setError] = useState<string | null>(null)
  const [streamingIds, setStreamingIds] = useState<Set<string>>(new Set())
  const agentRef = useRef<HttpAgent | null>(null)
  const messagesRef = useRef<Message[]>([])
  const statusRef = useRef<ChatStatus>('idle')
  const streamingRef = useRef<Set<string>>(new Set())
  const rafRef = useRef<number | null>(null)

  const publish = useCallback((nextMessages: Message[], nextStatus: ChatStatus) => {
    messagesRef.current = nextMessages
    statusRef.current = nextStatus
    if (rafRef.current != null) {
      return
    }
    rafRef.current = requestAnimationFrame(() => {
      rafRef.current = null
      setMessages(messagesRef.current)
      setStatus(statusRef.current)
      setStreamingIds(new Set(streamingRef.current))
    })
  }, [])

  const flush = useCallback(() => {
    if (rafRef.current != null) {
      cancelAnimationFrame(rafRef.current)
      rafRef.current = null
    }
    setMessages(messagesRef.current)
    setStatus(statusRef.current)
    setStreamingIds(new Set(streamingRef.current))
  }, [])

  const stop = useCallback(() => {
    agentRef.current?.abortRun()
    agentRef.current = null
    streamingRef.current = new Set()
    if (statusRef.current === 'streaming') {
      statusRef.current = 'idle'
    }
    flush()
  }, [flush])

  const load = useCallback(
    async (nextSessionId: number | null) => {
      stop()
      setError(null)
      setSessionId(nextSessionId)
      streamingRef.current = new Set()
      if (nextSessionId == null) {
        messagesRef.current = []
        statusRef.current = 'idle'
        flush()
        return
      }
      const history = await listMessages(nextSessionId)
      messagesRef.current = history
      const last = history.at(-1)
      statusRef.current = last && isConfirmActivity(last) ? 'awaiting_confirm' : 'idle'
      flush()
    },
    [flush, stop],
  )

  const consume = useCallback(
    async (agent: HttpAgent, resume?: ResumeEntry[]) => {
      agentRef.current = agent
      const scratch = createSubAgentScratch()
      statusRef.current = 'streaming'
      setStatus('streaming')

      const markStreaming = (id: string, on: boolean) => {
        const next = new Set(streamingRef.current)
        if (on) {
          next.add(id)
        } else {
          next.delete(id)
        }
        streamingRef.current = next
      }

      const subscriber: AgentSubscriber = {
        onMessagesChanged: ({ messages: protocol }) => {
          const named = cloneMessages(protocol).map((item) => withAgentName(item))
          publish(upsertAll(messagesRef.current, named), statusRef.current)
        },
        onTextMessageStartEvent: ({ event }) => {
          markStreaming(event.messageId, true)
        },
        onTextMessageEndEvent: ({ event }) => {
          markStreaming(event.messageId, false)
        },
        onReasoningMessageStartEvent: ({ event }) => {
          markStreaming(event.messageId, true)
        },
        onReasoningMessageEndEvent: ({ event }) => {
          markStreaming(event.messageId, false)
        },
        onCustomEvent: ({ event }) => {
          const next = applyCustomEvent(event, messagesRef.current, scratch)
          const last = next.at(-1)
          publish(next, isConfirmActivity(last) ? 'awaiting_confirm' : statusRef.current)
        },
        onRawEvent: ({ event }) => {
          publish(applyRawEvent(event, messagesRef.current, scratch), statusRef.current)
        },
        onRunFinishedEvent: (params) => {
          streamingRef.current = new Set()
          if (params.outcome === 'interrupt') {
            // Prefer official RUN_FINISHED interrupts so resume ids match backend pending.
            const withoutConfirm = messagesRef.current.filter((item) => !isConfirmActivity(item))
            const activity = confirmActivity(params.interrupts)
            publish(upsertAll(withoutConfirm, [activity]), 'awaiting_confirm')
            return
          }
          const last = messagesRef.current.at(-1)
          if (isConfirmActivity(last)) {
            publish(messagesRef.current, 'awaiting_confirm')
            return
          }
          publish(messagesRef.current, 'idle')
        },
        onRunErrorEvent: () => {
          streamingRef.current = new Set()
          publish(messagesRef.current, 'error')
        },
      }

      try {
        await runAgent(agent, {
          runId: crypto.randomUUID(),
          resume,
          subscriber,
        })
        if (statusRef.current === 'streaming') {
          streamingRef.current = new Set()
          publish(messagesRef.current, 'idle')
        }
        flush()
      } finally {
        agentRef.current = null
      }
    },
    [flush, publish],
  )

  const send = useCallback(
    async (activeSessionId: number, prompt: string) => {
      setError(null)
      const userMessage: UserMessage = {
        id: crypto.randomUUID(),
        role: 'user',
        content: prompt,
      }
      messagesRef.current = [...messagesRef.current, userMessage]
      statusRef.current = 'streaming'
      flush()
      const agent = createChatAgent(activeSessionId, userId, [userMessage])
      try {
        await consume(agent)
      } catch (err) {
        if (isAbortError(err)) {
          stop()
          return
        }
        setError(err instanceof Error ? err.message : '生成失败，请重试')
        streamingRef.current = new Set()
        statusRef.current = 'error'
        flush()
      }
    },
    [consume, flush, stop, userId],
  )

  const resume = useCallback(
    async (activeSessionId: number, approved: boolean) => {
      setError(null)
      const last = [...messagesRef.current].reverse().find(isConfirmActivity)
      const interrupts = confirmInterrupts(last)
      if (interrupts.length === 0) {
        setError('没有可恢复的工具确认')
        statusRef.current = 'error'
        flush()
        return
      }
      const resumeEntries: ResumeEntry[] = interrupts.map((item) => ({
        interruptId: item.id,
        status: 'resolved',
        payload: { approved },
      }))
      const agent = createChatAgent(activeSessionId, userId, [])
      try {
        await consume(agent, resumeEntries)
      } catch (err) {
        if (isAbortError(err)) {
          stop()
          return
        }
        setError(err instanceof Error ? err.message : '恢复失败，请重试')
        statusRef.current = 'error'
        flush()
      }
    },
    [consume, flush, stop, userId],
  )

  return { sessionId, messages, status, error, streamingIds, load, send, resume, stop }
}

function confirmActivity(interrupts: Interrupt[]): ActivityMessage {
  return {
    id: crypto.randomUUID(),
    role: 'activity',
    activityType: 'TOOL_CONFIRM',
    content: { interrupts },
    metadata: { agentName: 'general_chat' },
  }
}

function confirmInterrupts(message: Message | undefined): Array<{ id: string }> {
  if (!isConfirmActivity(message)) {
    return []
  }
  const raw = message.content.interrupts
  if (!Array.isArray(raw)) {
    return []
  }
  return raw.flatMap((item) => {
    if (!item || typeof item !== 'object') {
      return []
    }
    const id = (item as { id?: unknown }).id
    return typeof id === 'string' && id ? [{ id }] : []
  })
}
