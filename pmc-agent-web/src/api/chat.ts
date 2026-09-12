import { readSse } from './sse'
import type { ChatResponse, ToolFeedbackSubmit } from '../types/chat'

export async function* streamChat(
  sessionId: number,
  userId: string,
  prompt: string,
  signal: AbortSignal,
): AsyncGenerator<ChatResponse> {
  const response = await fetch('/stream', {
    method: 'POST',
    headers: { Accept: 'text/event-stream', 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionId, userId, prompt }),
    signal,
  })
  if (!response.ok) {
    throw new Error(await readError(response))
  }
  yield* parseChatSse(response)
}

export async function* resumeChat(
  sessionId: number,
  userId: string,
  toolFeedbacks: ToolFeedbackSubmit[],
  signal: AbortSignal,
): AsyncGenerator<ChatResponse> {
  const response = await fetch('/resume_stream', {
    method: 'POST',
    headers: { Accept: 'text/event-stream', 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionId, userId, toolFeedbacks }),
    signal,
  })
  if (!response.ok) {
    throw new Error(await readError(response))
  }
  yield* parseChatSse(response)
}

async function* parseChatSse(response: Response): AsyncGenerator<ChatResponse> {
  for await (const evt of readSse(response)) {
    if (!evt.data || evt.data === '{}') {
      continue
    }
    let parsed: ChatResponse
    try {
      parsed = JSON.parse(evt.data) as ChatResponse
    } catch {
      continue
    }
    if (evt.event === 'error' || parsed.error) {
      const message =
        typeof parsed.errorMessage === 'string' ? parsed.errorMessage : '生成失败，请重试'
      throw new Error(message)
    }
    yield parsed
  }
}

async function readError(response: Response): Promise<string> {
  const text = await response.text()
  return text || `请求失败（${response.status}）`
}
