import { useMemo, useState } from 'react'
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

  const onNew = async () => {
    const created = await conversations.create()
    await chat.load(created.id)
    setSidebarOpen(false)
  }

  const onSelect = async (sessionId: number) => {
    await chat.load(sessionId)
    setSidebarOpen(false)
  }

  const onDelete = async (sessionId: number) => {
    await conversations.remove(sessionId)
    if (chat.sessionId === sessionId) {
      await chat.load(null)
    }
  }

  const onSend = async (text: string) => {
    let activeId = chat.sessionId
    if (activeId == null) {
      const created = await conversations.create()
      activeId = created.id
      await chat.load(created.id)
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
              onConfirm={(approved) => void onConfirm(approved)}
            />
          </div>
        ) : (
          <EmptyState />
        )}
        {conversations.error ? <div className="error-banner">{conversations.error}</div> : null}
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
