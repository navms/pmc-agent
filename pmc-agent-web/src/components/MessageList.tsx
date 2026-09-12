import type { ChatMessage, ChatStatus, ToolFeedbackSubmit } from '../types/chat'
import { isLastAssistantOfTurn, usageForTurn } from '../lib/tokenUsage'
import { MessageItem } from './MessageItem'
import { ToolConfirmPanel } from './ToolConfirmPanel'

interface MessageListProps {
  messages: ChatMessage[]
  status: ChatStatus
  onConfirm: (feedbacks: ToolFeedbackSubmit[]) => void
}

export function MessageList({ messages, status, onConfirm }: MessageListProps) {
  return (
    <div className="thread-inner">
      {messages.map((message, index) => {
        const isLastConfirm =
          message.messageType === 'tool-confirm' && index === messages.length - 1
        const turnUsage =
          isLastAssistantOfTurn(messages, index) ? usageForTurn(messages, index) : undefined
        return (
          <div key={message.id}>
            <MessageItem message={message} turnUsage={turnUsage} />
            {isLastConfirm ? (
              <ToolConfirmPanel
                message={message}
                disabled={status === 'streaming'}
                onConfirm={onConfirm}
              />
            ) : null}
          </div>
        )
      })}
    </div>
  )
}
