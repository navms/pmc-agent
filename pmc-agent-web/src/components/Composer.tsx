import { useRef, type KeyboardEvent } from 'react'
import type { ChatStatus, TokenUsage } from '../types/chat'
import { formatSessionUsage } from '../lib/tokenUsage'

interface ComposerProps {
  status: ChatStatus
  error: string | null
  disabled: boolean
  sessionUsage?: TokenUsage
  onSend: (text: string) => void
  onStop: () => void
}

export function Composer({
  status,
  error,
  disabled,
  sessionUsage,
  onSend,
  onStop,
}: ComposerProps) {
  const ref = useRef<HTMLTextAreaElement>(null)
  const streaming = status === 'streaming'

  const submit = () => {
    const value = ref.current?.value.trim() ?? ''
    if (!value || disabled || streaming) {
      return
    }
    onSend(value)
    if (ref.current) {
      ref.current.value = ''
      ref.current.style.height = 'auto'
    }
  }

  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      submit()
    }
  }

  const onInput = () => {
    const el = ref.current
    if (!el) {
      return
    }
    el.style.height = 'auto'
    el.style.height = `${Math.min(el.scrollHeight, 200)}px`
  }

  return (
    <div className="composer-wrap">
      {error ? <div className="error-banner">{error}</div> : null}
      <div className="composer">
        <textarea
          ref={ref}
          rows={1}
          placeholder={status === 'awaiting_confirm' ? '请先处理工具确认' : '给 PMC Agent 发送消息'}
          disabled={disabled}
          onKeyDown={onKeyDown}
          onInput={onInput}
        />
        <div className="composer-bar">
          <span className="hint">
            Enter 发送 · Shift+Enter 换行
            {sessionUsage ? (
              <>
                <span className="hint-sep" aria-hidden>
                  ·
                </span>
                <span className="token-meter">{formatSessionUsage(sessionUsage)}</span>
              </>
            ) : null}
          </span>
          {streaming ? (
            <button type="button" className="send" onClick={onStop} aria-label="停止生成">
              ■
            </button>
          ) : (
            <button type="button" className="send" onClick={submit} disabled={disabled} aria-label="发送">
              ↑
            </button>
          )}
        </div>
        {streaming ? <div className="progress" /> : null}
      </div>
    </div>
  )
}
