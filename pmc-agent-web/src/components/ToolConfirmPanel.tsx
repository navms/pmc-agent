import type { ActivityMessage } from '@ag-ui/client'

interface ToolConfirmPanelProps {
  message: ActivityMessage
  disabled: boolean
  onConfirm: (approved: boolean) => void
}

export function ToolConfirmPanel({ message, disabled, onConfirm }: ToolConfirmPanelProps) {
  const items = interruptLabels(message)

  return (
    <div className="confirm">
      <h3>Agent 请求执行工具，是否继续？</h3>
      {items.length > 0 ? (
        <ul className="confirm-list">
          {items.map((item) => (
            <li key={item.id}>{item.label}</li>
          ))}
        </ul>
      ) : null}
      <div className="confirm-actions">
        <button type="button" className="btn-approve" disabled={disabled} onClick={() => onConfirm(true)}>
          批准
        </button>
        <button type="button" className="btn-reject" disabled={disabled} onClick={() => onConfirm(false)}>
          拒绝
        </button>
      </div>
    </div>
  )
}

function interruptLabels(message: ActivityMessage): Array<{ id: string; label: string }> {
  const raw = message.content.interrupts
  if (!Array.isArray(raw)) {
    return []
  }
  return raw.flatMap((item) => {
    if (!item || typeof item !== 'object') {
      return []
    }
    const row = item as Record<string, unknown>
    if (typeof row.id !== 'string' || !row.id) {
      return []
    }
    const metadata =
      row.metadata && typeof row.metadata === 'object' ? (row.metadata as Record<string, unknown>) : {}
    const label =
      typeof metadata.toolName === 'string' && metadata.toolName
        ? metadata.toolName
        : typeof row.message === 'string' && row.message
          ? row.message
          : '工具调用'
    return [{ id: row.id, label }]
  })
}
