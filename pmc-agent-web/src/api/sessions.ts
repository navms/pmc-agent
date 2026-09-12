import type { ChatSession, Message } from '../types/chat'

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

export async function listMessages(sessionId: number): Promise<Message[]> {
  const response = await fetch(`/sessions/${sessionId}/messages`)
  if (!response.ok) {
    throw new Error('无法加载消息')
  }
  return (await response.json()) as Message[]
}

export async function deleteSession(sessionId: number): Promise<void> {
  const response = await fetch(`/sessions/${sessionId}`, {
    method: 'DELETE',
  })
  if (!response.ok && response.status !== 204) {
    throw new Error('无法删除会话')
  }
}
