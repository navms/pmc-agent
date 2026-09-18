import { useMemo, useState } from 'react'
import { submitSessionFeedback } from './api/sessions'
import { Composer } from './components/Composer'
import { EmptyState } from './components/EmptyState'
import { MessageList } from './components/MessageList'
import { Sidebar } from './components/Sidebar'
import { useChatSession } from './hooks/useChatSession'
import { useConversations } from './hooks/useConversations'
import { getUserId } from './lib/id'
import { sessionUsage } from './lib/tokenUsage'
import './styles/tokens.css'

export default function App() {
  const userId = useMemo(() => getUserId(), [])
  const conversations = useConversations(userId)
  const chat = useChatSession(userId)
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [dislikedIds, setDislikedIds] = useState<Set<string>>(() => new Set())
  const [dislikingId, setDislikingId] = useState<string | null>(null)
  const [feedbackError, setFeedbackError] = useState<string | null>(null)

  const onNew = async () => {
    const created = await conversations.create()
    await chat.load(created.id)
    setDislikedIds(new Set())
    setDislikingId(null)
    setFeedbackError(null)
    setSidebarOpen(false)
  }

  const onSelect = async (sessionId: number) => {
    await chat.load(sessionId)
    setDislikedIds(new Set())
    setDislikingId(null)
    setFeedbackError(null)
    setSidebarOpen(false)
  }

  const onDelete = async (sessionId: number) => {
    await conversations.remove(sessionId)
    if (chat.sessionId === sessionId) {
      await chat.load(null)
      setDislikedIds(new Set())
      setDislikingId(null)
      setFeedbackError(null)
    }
  }

  const onSend = async (text: string) => {
    let activeId = chat.sessionId
    if (activeId == null) {
      const created = await conversations.create()
      activeId = created.id
      await chat.load(created.id)
      setDislikedIds(new Set())
      setDislikingId(null)
      setFeedbackError(null)
    }
    await chat.send(activeId, text)
    await conversations.refresh()
  }

  const onConfirm = async (approved: boolean) => {
    if (chat.sessionId == null) {
      return
    }
    await chat.resume(chat.sessionId, approved)
    await conversations.refresh()
  }

  const onDislike = async (messageId: string) => {
    if (chat.sessionId == null || dislikedIds.has(messageId) || dislikingId) {
      return
    }
    setFeedbackError(null)
    setDislikingId(messageId)
    try {
      await submitSessionFeedback(chat.sessionId, messageId)
      setDislikedIds((current) => new Set(current).add(messageId))
    } catch (error) {
      setFeedbackError(error instanceof Error ? error.message : '无法提交反馈')
    } finally {
      setDislikingId(null)
    }
  }

  const hasMessages = chat.messages.length > 0
  const composerDisabled = chat.status === 'awaiting_confirm'
  const usage = useMemo(() => sessionUsage(chat.messages), [chat.messages])

  return (
    <div className="app-shell">
      <Sidebar
        sessions={conversations.sessions}
        activeSessionId={chat.sessionId}
        open={sidebarOpen}
        onNew={() => void onNew()}
        onSelect={(id) => void onSelect(id)}
        onDelete={(id) => void onDelete(id)}
      />
      <main className={hasMessages ? 'main' : 'main is-empty'}>
        <div className="mobile-bar">
          <button type="button" className="ghost-btn" onClick={() => setSidebarOpen(true)}>
            会话
          </button>
          <strong>Agent</strong>
        </div>
        {hasMessages ? (
          <div className="thread">
            <MessageList
              messages={chat.messages}
              status={chat.status}
              streamingIds={chat.streamingIds}
              clarifying={chat.clarifying}
              dislikedIds={dislikedIds}
              dislikingId={dislikingId}
              onConfirm={(approved) => void onConfirm(approved)}
              onDislike={(messageId) => void onDislike(messageId)}
            />
          </div>
        ) : (
          <EmptyState />
        )}
        {conversations.error ? <div className="error-banner">{conversations.error}</div> : null}
        {feedbackError ? <div className="error-banner">{feedbackError}</div> : null}
        <Composer
          status={chat.status}
          error={chat.error}
          disabled={composerDisabled}
          sessionUsage={usage}
          onSend={(text) => void onSend(text)}
          onStop={chat.stop}
        />
      </main>
    </div>
  )
}
