import { useState } from 'react'
import type { Message, TokenUsage } from '../types/chat'
import { agentToolLabel } from '../lib/agentTools'
import { textContent } from '../lib/agui/message'
import { MessageItem } from './MessageItem'

interface SubAgentCardProps {
  agentName: string
  children: Array<{ message: Message; index: number }>
  usageForIndex: (index: number) => TokenUsage | undefined
  streamingIds: Set<string>
  dislikedIds: Set<string>
  dislikingId: string | null
  onDislike: (messageId: string) => void
  defaultOpen?: boolean
}

export function SubAgentCard({
  agentName,
  children,
  usageForIndex,
  streamingIds,
  dislikedIds,
  dislikingId,
  onDislike,
  defaultOpen = true,
}: SubAgentCardProps) {
  const [open, setOpen] = useState(defaultOpen)
  const summary =
    children
      .filter((item) => item.message.role === 'assistant')
      .map((item) => textContent(item.message).trim())
      .find(Boolean) ?? ''
  const summaryLine = summary ? summary.replace(/\s+/g, ' ').slice(0, 72) : '内部调用与回复'

  return (
    <section className="sub-agent-card">
      <button
        type="button"
        className="sub-agent-card__header"
        onClick={() => setOpen((value) => !value)}
        aria-expanded={open}
      >
        <span className="sub-agent-card__title">
          子 Agent · {agentToolLabel(agentName)}
          <span className="sub-agent-card__name">{agentName}</span>
        </span>
        <span className="sub-agent-card__summary">{open ? '' : summaryLine}</span>
        <span className="sub-agent-card__chevron" aria-hidden>
          {open ? '收起' : '展开'}
        </span>
      </button>
      {open ? (
        <div className="sub-agent-card__body">
          {children.map(({ message, index }) => (
            <div key={message.id} className="sub-agent-card__item">
              <MessageItem
                message={message}
                turnUsage={usageForIndex(index)}
                nested
                streaming={streamingIds.has(message.id)}
                disliked={dislikedIds.has(message.id)}
                disliking={dislikingId === message.id}
                onDislike={onDislike}
              />
            </div>
          ))}
        </div>
      ) : null}
    </section>
  )
}
