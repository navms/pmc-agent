import type { Message } from '@ag-ui/client'

export type ChatStatus = 'idle' | 'streaming' | 'awaiting_confirm' | 'error'

export type { Message }

export interface TokenUsage {
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
}

export interface ChatSession {
  id: number
  title: string | null
  status: string
  updatedAt: string
}
