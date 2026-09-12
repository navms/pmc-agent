import { useCallback, useEffect, useState } from 'react'
import { createSession, deleteSession, listSessions } from '../api/sessions'
import type { ChatSession } from '../types/chat'

export function useConversations(userId: string) {
  const [sessions, setSessions] = useState<ChatSession[]>([])
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    const list = await listSessions(userId)
    setSessions(list)
    setError(null)
  }, [userId])

  useEffect(() => {
    let cancelled = false
    listSessions(userId)
      .then((list) => {
        if (!cancelled) {
          setSessions(list)
          setError(null)
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : '无法加载会话')
        }
      })
    return () => {
      cancelled = true
    }
  }, [userId])

  const create = useCallback(async () => {
    const created = await createSession(userId)
    setSessions((prev) => [created, ...prev])
    return created
  }, [userId])

  const remove = useCallback(async (sessionId: number) => {
    await deleteSession(sessionId)
    setSessions((prev) => prev.filter((item) => item.id !== sessionId))
  }, [])

  return { sessions, error, refresh, create, remove }
}
