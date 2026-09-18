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
  disliked = false,
  disliking = false,
  onDislike,
}: {
  message: AssistantMessage
  turnUsage?: TokenUsage
  nested?: boolean
  streaming?: boolean
  disliked?: boolean
  disliking?: boolean
  onDislike?: (messageId: string) => void
}) {
  const text = textContent(message)
  const showDislike = !streaming && Boolean(onDislike)
  const dislikeBar = showDislike ? (
    <div className="bubble-actions">
      <button
        type="button"
        className={`bubble-action${disliked ? ' is-active' : ''}`}
        disabled={disliked || disliking}
        aria-label="踩"
        onClick={() => onDislike?.(message.id)}
      >
        {disliking ? '提交中' : disliked ? '已踩' : '踩'}
      </button>
    </div>
  ) : null
  return (
    <div>
      {message.toolCalls?.length ? <ToolCallsBlock message={message} nested={nested} /> : null}
      {text || streaming || !message.toolCalls?.length ? (
        <article className={`bubble assistant${nested ? ' nested' : ''}`}>
          <div className="bubble-body">
            <MarkdownBody text={text || (streaming ? '…' : '')} />
          </div>
          {turnUsage ? <div className="bubble-usage">{formatTurnUsage(turnUsage)}</div> : null}
          {dislikeBar}
        </article>
      ) : (
        <>
          {turnUsage ? <div className="bubble-usage">{formatTurnUsage(turnUsage)}</div> : null}
          {dislikeBar}
        </>
      )}
    </div>
  )
}
