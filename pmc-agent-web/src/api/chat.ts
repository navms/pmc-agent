import { FilterToolCallsMiddleware, HttpAgent } from '@ag-ui/client'
import type { ResumeEntry } from '@ag-ui/core'
import { HIDDEN_PARENT_TOOLS } from '../lib/agui/hiddenTools'

const AGENT_ID = 'pmc_supervisor'

export function createChatAgent(sessionId: number, userId: string, initialMessages: HttpAgent['messages'] = []) {
  const agent = new HttpAgent({
    url: '/agui/run',
    threadId: String(sessionId),
    headers: {
      'X-User-Id': userId,
      'X-Agent-Id': AGENT_ID,
    },
    initialMessages,
  })
  agent.use(new FilterToolCallsMiddleware({ disallowedToolCalls: [...HIDDEN_PARENT_TOOLS] }))
  return agent
}

export async function runAgent(
  agent: HttpAgent,
  options: {
    runId: string
    resume?: ResumeEntry[]
    subscriber: NonNullable<Parameters<HttpAgent['runAgent']>[1]>
  },
): Promise<void> {
  await agent.runAgent({ runId: options.runId, resume: options.resume }, options.subscriber)
}

export function isAbortError(error: unknown): boolean {
  return (
    (error instanceof DOMException && error.name === 'AbortError') ||
    (error instanceof Error && (error.name === 'AbortError' || /abort/i.test(error.message)))
  )
}
