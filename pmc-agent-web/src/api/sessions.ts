import type { ChatMessage, ChatSession } from '../types/chat'
import { parseTokenUsage } from '../lib/tokenUsage'

export async function listSessions(userId: string): Promise<ChatSession[]> {
  const response = await fetch(`/sessions?userId=${encodeURIComponent(userId)}`)
  if (!response.ok) {
    throw new Error('无法加载会话列表')
  }
  return response.json() as Promise<ChatSession[]>
}

export async function createSession(userId: string): Promise<ChatSession> {
  const response = await fetch('/sessions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId }),
  })
  if (!response.ok) {
    throw new Error('无法创建会话')
  }
  return response.json() as Promise<ChatSession>
}

export async function listMessages(sessionId: number): Promise<ChatMessage[]> {
  const response = await fetch(`/sessions/${sessionId}/messages`)
  if (!response.ok) {
    throw new Error('无法加载消息')
  }
  const rows = (await response.json()) as Array<{
    id: number
    messageType: ChatMessage['messageType']
    content: string
    payload?: ChatMessage['payload']
    createdAt?: string
  }>
  return rows.map((row) => {
    const agentName =
      typeof row.payload?.agentName === 'string' ? row.payload.agentName : undefined
    return {
      id: String(row.id),
      messageType: row.messageType,
      content: row.content ?? '',
      payload: row.payload,
      agentName,
      tokenUsage: parseTokenUsage(row.payload?.tokenUsage),
      createdAt: row.createdAt,
    }
  })
}

export async function renameSession(sessionId: number, title: string): Promise<ChatSession> {
  const response = await fetch(`/sessions/${sessionId}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title }),
  })
  if (!response.ok) {
    throw new Error('无法更新标题')
  }
  return response.json() as Promise<ChatSession>
}

export async function deleteSession(sessionId: number): Promise<void> {
  const response = await fetch(`/sessions/${sessionId}`, {
    method: 'DELETE',
  })
  if (!response.ok && response.status !== 204) {
    throw new Error('无法删除会话')
  }
}
