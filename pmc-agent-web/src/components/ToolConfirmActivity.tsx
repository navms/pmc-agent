import type { ActivityMessage } from '@ag-ui/client'
import { ToolConfirmPanel } from './ToolConfirmPanel'

export function ToolConfirmActivity({
  message,
  nested = false,
  showActions = false,
  disabled = false,
  onConfirm,
}: {
  message: ActivityMessage
  nested?: boolean
  showActions?: boolean
  disabled?: boolean
  onConfirm?: (approved: boolean) => void
}) {
  return (
    <div>
      <details className={`tool-block${nested ? ' nested' : ''}`}>
        <summary>需要确认的工具调用</summary>
        <pre>{JSON.stringify(message.content, null, 2)}</pre>
      </details>
      {showActions && onConfirm ? (
        <ToolConfirmPanel message={message} disabled={disabled} onConfirm={onConfirm} />
      ) : null}
    </div>
  )
}
