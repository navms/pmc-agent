import type { AssistantMessage } from '@ag-ui/client'
import { SUB_AGENT_NAMES } from '../lib/agentTools'
import { agentNameOf } from '../lib/agui/message'

export function ToolCallsBlock({
  message,
  nested = false,
}: {
  message: AssistantMessage
  nested?: boolean
}) {
  const names = message.toolCalls?.map((item) => item.function.name).filter(Boolean) ?? []
  return (
    <details className={`tool-block${nested ? ' nested' : ''}`}>
      <summary>调用工具 {formatToolSummary(agentNameOf(message), names)}</summary>
      <pre>{JSON.stringify(message.toolCalls, null, 2)}</pre>
    </details>
  )
}

export function formatToolSummary(agentName: string | undefined, names: string[]): string {
  if (!names.length) {
    return ''
  }
  const tools = names.join(', ')
  if (agentName && SUB_AGENT_NAMES.has(agentName) && !SUB_AGENT_NAMES.has(names[0])) {
    return `· ${agentName} › ${tools}`
  }
  return `· ${tools}`
}
