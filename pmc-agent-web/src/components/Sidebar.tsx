import type { ChatSession } from '../types/chat'

interface SidebarProps {
  sessions: ChatSession[]
  activeSessionId: number | null
  open: boolean
  onNew: () => void
  onSelect: (sessionId: number) => void
  onDelete: (sessionId: number) => void
}

export function Sidebar({
  sessions,
  activeSessionId,
  open,
  onNew,
  onSelect,
  onDelete,
}: SidebarProps) {
  return (
    <aside className={open ? 'sidebar open' : 'sidebar'}>
      <div className="brand">
        <div className="brand-mark" aria-hidden>
          PMC
        </div>
        <div className="brand-copy">
          <strong>PMC Agent</strong>
          <span>生产计划助手</span>
        </div>
      </div>
      <button type="button" className="new-chat" onClick={onNew}>
        + 开启新对话
      </button>
      <div className="session-list">
        {sessions.map((session) => (
          <div
            key={session.id}
            className={session.id === activeSessionId ? 'session-item active' : 'session-item'}
          >
            <button type="button" className="session-title" onClick={() => onSelect(session.id)}>
              {session.title || '新对话'}
            </button>
            <button
              type="button"
              className="session-delete"
              aria-label="删除会话"
              onClick={() => onDelete(session.id)}
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </aside>
  )
}
