import { useState } from 'react'
import type { ChatMessage, TokenUsage } from '../types/chat'
import { agentToolLabel } from '../lib/agentTools'
import { MessageItem } from './MessageItem'

interface SubAgentCardProps {
  agentName: string
  children: Array<{ message: ChatMessage; index: number }>
  toolResult?: { message: ChatMessage; index: number }
  /** 与 MessageList 一致：仅轮次末条助手展示用量 */
  usageForIndex: (index: number) => TokenUsage | undefined
  /** 本轮流式产生的卡片默认展开 */
  defaultOpen?: boolean
}

/**
 * 子 Agent 回合内嵌卡片：内部 tool 轨迹 + 自然语言回复。
 */
export function SubAgentCard({
  agentName,
  children,
  toolResult,
  usageForIndex,
  defaultOpen = true,
}: SubAgentCardProps) {
  const [open, setOpen] = useState(defaultOpen)
  const summary =
    children
      .filter((item) => item.message.messageType === 'assistant')
      .map((item) => item.message.content.trim())
      .find(Boolean) ?? ''
  const summaryLine = summary ? summary.replace(/\s+/g, ' ').slice(0, 72) : '内部调用与回复'

  return (
    <section className={`sub-agent-card${open ? ' open' : ''}`}>
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
              <MessageItem message={message} turnUsage={usageForIndex(index)} nested />
            </div>
          ))}
          {toolResult ? (
            <details className="sub-agent-card__tool-result">
              <summary>调用结果 · {agentName}</summary>
              <MessageItem message={toolResult.message} nested />
            </details>
          ) : null}
        </div>
      ) : null}
    </section>
  )
}
