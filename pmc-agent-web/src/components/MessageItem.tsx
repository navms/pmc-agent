import type { Message } from '@ag-ui/client'
import type { TokenUsage } from '../types/chat'
import { textContent } from '../lib/agui/message'
import { AssistantBubble } from './AssistantBubble'
import { ReasoningBlock } from './ReasoningBlock'
import { ToolConfirmActivity } from './ToolConfirmActivity'
import { ToolResultBlock } from './ToolResultBlock'
import { UserBubble } from './UserBubble'

interface MessageItemProps {
  message: Message
  turnUsage?: TokenUsage
  nested?: boolean
  streaming?: boolean
  showConfirm?: boolean
  confirmDisabled?: boolean
  onConfirm?: (approved: boolean) => void
}

export function MessageItem({
  message,
  turnUsage,
  nested = false,
  streaming = false,
  showConfirm = false,
  confirmDisabled = false,
  onConfirm,
}: MessageItemProps) {
  try {
    return renderMessage(message, {
      turnUsage,
      nested,
      streaming,
      showConfirm,
      confirmDisabled,
      onConfirm,
    })
  } catch (error) {
    console.error('MessageItem render failed', message.id, error)
    return (
      <details className="tool-block" open>
        <summary>消息渲染失败</summary>
        <pre>{textContent(message) || String(error)}</pre>
      </details>
    )
  }
}

function renderMessage(
  message: Message,
  options: Omit<MessageItemProps, 'message'>,
) {
  if (message.role === 'user') {
    return <UserBubble message={message} />
  }
  if (message.role === 'reasoning') {
    return <ReasoningBlock message={message} nested={options.nested} streaming={options.streaming} />
  }
  if (message.role === 'assistant') {
    return (
      <AssistantBubble
        message={message}
        turnUsage={options.turnUsage}
        nested={options.nested}
        streaming={options.streaming}
      />
    )
  }
  if (message.role === 'tool') {
    return <ToolResultBlock message={message} nested={options.nested} />
  }
  if (message.role === 'activity' && message.activityType === 'TOOL_CONFIRM') {
    return (
      <ToolConfirmActivity
        message={message}
        nested={options.nested}
        showActions={options.showConfirm}
        disabled={options.confirmDisabled}
        onConfirm={options.onConfirm}
      />
    )
  }
  return null
}
