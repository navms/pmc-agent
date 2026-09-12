export type ChatStatus = 'idle' | 'streaming' | 'awaiting_confirm' | 'error'

export type MessageType =
  | 'user'
  | 'assistant'
  | 'tool'
  | 'tool-request'
  | 'tool-confirm'

export interface ToolCall {
  id?: string
  type?: string
  name?: string
  arguments?: string
}

export interface ToolResult {
  id?: string
  name?: string
  responseData?: string
}

export interface ToolFeedback {
  id?: string
  name?: string
  arguments?: string
  description?: string
  result?: string
}

export interface TokenUsage {
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
}

export interface MessagePayload {
  messageType?: MessageType
  content?: string
  toolCalls?: ToolCall[]
  responses?: ToolResult[]
  toolFeedback?: ToolFeedback[]
  toolsAutomaticallyApproved?: ToolCall[]
  tokenUsage?: TokenUsage
  [key: string]: unknown
}

export interface ChatResponse {
  node?: string
  agentName?: string
  messageResponse?: MessagePayload | null
  chunk?: string
  interrupted?: boolean
  tokenUsage?: TokenUsage
  error?: unknown
  errorMessage?: string
}

export interface ChatSession {
  id: number
  title: string | null
  status: string
  updatedAt: string
}

export interface ChatMessage {
  id: string
  messageType: MessageType
  content: string
  payload?: MessagePayload | null
  /** 产生该消息的 Agent；子 Agent 内部 tool 时为 query_bank 等 */
  agentName?: string
  /** 图节点；子 Agent 轨迹为 _SUB_AGENT_TOOL_ / _SUB_AGENT_NARRATIVE_ */
  node?: string
  tokenUsage?: TokenUsage
  streaming?: boolean
  createdAt?: string
}

export interface ToolFeedbackSubmit {
  id?: string
  name?: string
  arguments?: string
  description?: string
  result: 'APPROVED' | 'REJECTED'
}
