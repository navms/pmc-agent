import type { ChatStatus, Message } from '../types/chat'
import { groupMessagesForRender } from '../lib/groupMessages'
import { isConfirmActivity } from '../lib/agui/message'
import { isLastAssistantOfTurn, usageForTurn } from '../lib/tokenUsage'
import { MessageItem } from './MessageItem'
import { SubAgentCard } from './SubAgentCard'

interface MessageListProps {
  messages: Message[]
  status: ChatStatus
  streamingIds: Set<string>
  clarifying?: boolean
  dislikedIds: Set<string>
  dislikingId: string | null
  onConfirm: (approved: boolean) => void
  onDislike: (messageId: string) => void
}

export function MessageList({
  messages,
  status,
  streamingIds,
  clarifying = false,
  dislikedIds,
  dislikingId,
  onConfirm,
  onDislike,
}: MessageListProps) {
  const items = groupMessagesForRender(messages)
  const usageForIndex = (index: number) =>
    isLastAssistantOfTurn(messages, index) ? usageForTurn(messages, index) : undefined

  return (
    <div className="thread-inner">
      {items.map((item) => {
        if (item.kind === 'single') {
          const { message, index } = item
          const isLastConfirm = isConfirmActivity(message) && index === messages.length - 1
          return (
            <div key={message.id}>
              <MessageItem
                message={message}
                turnUsage={usageForIndex(index)}
                streaming={streamingIds.has(message.id)}
                showConfirm={isLastConfirm}
                confirmDisabled={status === 'streaming'}
                disliked={dislikedIds.has(message.id)}
                disliking={dislikingId === message.id}
                onConfirm={onConfirm}
                onDislike={onDislike}
              />
            </div>
          )
        }

        const hasStreamingChild = item.children.some(({ message }) => streamingIds.has(message.id))
        const key = item.children[0]?.message.id ?? `sub-${item.agentName}`

        return (
          <SubAgentCard
            key={`sub-${item.agentName}-${key}`}
            agentName={item.agentName}
            children={item.children}
            usageForIndex={usageForIndex}
            streamingIds={streamingIds}
            dislikedIds={dislikedIds}
            dislikingId={dislikingId}
            onDislike={onDislike}
            defaultOpen={hasStreamingChild || status === 'streaming'}
          />
        )
      })}
      {clarifying ? (
        <div className="clarify-loading" role="status" aria-live="polite">
          <span className="clarify-loading__dot" aria-hidden />
          正在进行意图澄清…
        </div>
      ) : null}
    </div>
  )
}
