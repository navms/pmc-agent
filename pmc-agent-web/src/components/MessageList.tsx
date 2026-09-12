import type { ChatMessage, ChatStatus, ToolFeedbackSubmit } from '../types/chat'
import { groupMessagesForRender } from '../lib/groupMessages'
import { isLastAssistantOfTurn, usageForTurn } from '../lib/tokenUsage'
import { MessageItem } from './MessageItem'
import { SubAgentCard } from './SubAgentCard'
import { ToolConfirmPanel } from './ToolConfirmPanel'

interface MessageListProps {
  messages: ChatMessage[]
  status: ChatStatus
  onConfirm: (feedbacks: ToolFeedbackSubmit[]) => void
}

export function MessageList({ messages, status, onConfirm }: MessageListProps) {
  const items = groupMessagesForRender(messages)
  const usageForIndex = (index: number) =>
    isLastAssistantOfTurn(messages, index) ? usageForTurn(messages, index) : undefined

  return (
    <div className="thread-inner">
      {items.map((item) => {
        if (item.kind === 'single') {
          const { message, index } = item
          const isLastConfirm =
            message.messageType === 'tool-confirm' && index === messages.length - 1
          return (
            <div key={message.id}>
              <MessageItem message={message} turnUsage={usageForIndex(index)} />
              {isLastConfirm ? (
                <ToolConfirmPanel
                  message={message}
                  disabled={status === 'streaming'}
                  onConfirm={onConfirm}
                />
              ) : null}
            </div>
          )
        }

        const hasStreamingChild = item.children.some(({ message }) => message.streaming)
        const key =
          item.children[0]?.message.id ??
          item.toolResult?.message.id ??
          `sub-${item.agentName}`

        return (
          <SubAgentCard
            key={`sub-${item.agentName}-${key}`}
            agentName={item.agentName}
            children={item.children}
            toolResult={item.toolResult}
            usageForIndex={usageForIndex}
            defaultOpen={hasStreamingChild || status === 'streaming'}
          />
        )
      })}
    </div>
  )
}
