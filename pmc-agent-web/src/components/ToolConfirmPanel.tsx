import type { ChatMessage, ToolFeedbackSubmit } from '../types/chat'

interface ToolConfirmPanelProps {
  message: ChatMessage
  disabled: boolean
  onConfirm: (feedbacks: ToolFeedbackSubmit[]) => void
}

export function ToolConfirmPanel({ message, disabled, onConfirm }: ToolConfirmPanelProps) {
  const items = message.payload?.toolFeedback ?? []

  const decide = (result: 'APPROVED' | 'REJECTED') => {
    const feedbacks: ToolFeedbackSubmit[] = items.map((item) => ({
      id: item.id,
      name: item.name,
      arguments: item.arguments,
      description: result === 'REJECTED' ? '用户拒绝了该工具调用' : item.description,
      result,
    }))
    if (feedbacks.length === 0) {
      feedbacks.push({
        result,
        description: result === 'REJECTED' ? '用户拒绝了该工具调用' : undefined,
      })
    }
    onConfirm(feedbacks)
  }

  return (
    <div className="confirm">
      <h3>Agent 请求执行工具，是否继续？</h3>
      <div className="confirm-actions">
        <button
          type="button"
          className="btn-approve"
          disabled={disabled}
          onClick={() => decide('APPROVED')}
        >
          批准
        </button>
        <button
          type="button"
          className="btn-reject"
          disabled={disabled}
          onClick={() => decide('REJECTED')}
        >
          拒绝
        </button>
      </div>
    </div>
  )
}
