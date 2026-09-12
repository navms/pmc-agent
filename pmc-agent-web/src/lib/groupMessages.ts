import type { Message } from '@ag-ui/client'
import { isSubAgentName } from './agentTools'
import { agentNameOf } from './agui/message'

export type RenderItem =
  | { kind: 'single'; message: Message; index: number }
  | {
      kind: 'subAgent'
      agentName: string
      children: Array<{ message: Message; index: number }>
    }

/**
 * 将扁平消息按 agent name 归组：连续同一子 Agent 收进一张卡片。
 */
export function groupMessagesForRender(messages: Message[]): RenderItem[] {
  const items: RenderItem[] = []
  let index = 0
  while (index < messages.length) {
    const message = messages[index]
    const agentName = agentNameOf(message)
    if (!isSubAgentName(agentName)) {
      items.push({ kind: 'single', message, index })
      index += 1
      continue
    }

    const children: Array<{ message: Message; index: number }> = []
    while (index < messages.length && agentNameOf(messages[index]) === agentName) {
      children.push({ message: messages[index], index })
      index += 1
    }
    items.push({ kind: 'subAgent', agentName: agentName as string, children })
  }
  return items
}
