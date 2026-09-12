import type { UserMessage } from '@ag-ui/client'
import { textContent } from '../lib/agui/message'

export function UserBubble({ message }: { message: UserMessage }) {
  return (
    <article className="bubble user">
      <div className="bubble-body">{textContent(message)}</div>
    </article>
  )
}
