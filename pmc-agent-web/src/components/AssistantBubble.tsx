import type { AssistantMessage } from '@ag-ui/client'
import type { TokenUsage } from '../types/chat'
import { textContent } from '../lib/agui/message'
import { formatTurnUsage } from '../lib/tokenUsage'
import { MarkdownBody } from './MarkdownBody'
import { ToolCallsBlock } from './ToolCallsBlock'

export function AssistantBubble({
  message,
  turnUsage,
  nested = false,
  streaming = false,
}: {
  message: AssistantMessage
  turnUsage?: TokenUsage
  nested?: boolean
  streaming?: boolean
}) {
  const text = textContent(message)
  return (
    <div>
      {message.toolCalls?.length ? <ToolCallsBlock message={message} nested={nested} /> : null}
      {text || streaming || !message.toolCalls?.length ? (
        <article className={`bubble assistant${nested ? ' nested' : ''}`}>
          <div className="bubble-body">
            <MarkdownBody text={text || (streaming ? '…' : '')} />
          </div>
          {turnUsage ? <div className="bubble-usage">{formatTurnUsage(turnUsage)}</div> : null}
        </article>
      ) : turnUsage ? (
        <div className="bubble-usage">{formatTurnUsage(turnUsage)}</div>
      ) : null}
    </div>
  )
}
