import type { ReasoningMessage } from '@ag-ui/client'
import { textContent } from '../lib/agui/message'
import { MarkdownBody } from './MarkdownBody'

export function ReasoningBlock({
  message,
  nested = false,
  streaming = false,
}: {
  message: ReasoningMessage
  nested?: boolean
  streaming?: boolean
}) {
  return (
    <details className={`thinking-block${nested ? ' nested' : ''}`}>
      <summary>思考过程{streaming ? '…' : ''}</summary>
      <div className="bubble-body">
        <MarkdownBody text={textContent(message) || (streaming ? '…' : '')} />
      </div>
    </details>
  )
}
