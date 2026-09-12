import Markdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import type { ChatMessage, TokenUsage, ToolResult } from '../types/chat'
import { ChartView } from './ChartView'
import { formatTurnUsage } from '../lib/tokenUsage'

interface MessageItemProps {
  message: ChatMessage
  turnUsage?: TokenUsage
}

export function MessageItem({ message, turnUsage }: MessageItemProps) {
  try {
    return renderMessage(message, turnUsage)
  } catch (error) {
    console.error('MessageItem render failed', message.id, error)
    return (
      <details className="tool-block" open>
        <summary>消息渲染失败</summary>
        <pre>{message.content || String(error)}</pre>
      </details>
    )
  }
}

function renderMessage(message: ChatMessage, turnUsage?: TokenUsage) {
  if (message.messageType === 'user') {
    return (
      <article className="bubble user">
        <div className="bubble-body">{message.content}</div>
      </article>
    )
  }
  if (message.messageType === 'assistant') {
    return (
      <article className="bubble assistant">
        <div className="bubble-body">
          <MarkdownBody text={message.content || (message.streaming ? '…' : '')} />
        </div>
        {turnUsage ? <div className="bubble-usage">{formatTurnUsage(turnUsage)}</div> : null}
      </article>
    )
  }
  if (message.messageType === 'tool-request') {
    return (
      <details className="tool-block">
        <summary>调用工具 {formatToolSummary(message)}</summary>
        <pre>{formatJson(message.payload?.toolCalls ?? message.payload)}</pre>
      </details>
    )
  }
  if (message.messageType === 'tool') {
    const artifacts = collectArtifacts(message.payload?.responses)
    const displayPayload =
      truncateToolResponsesForDisplay(
        compactArtifactResponsesForDisplay(
          compactToolResponsesForDisplay(message.payload?.responses),
        ),
      ) ?? message.payload
    return (
      <div className="tool-with-artifacts">
        {artifacts.map((item, index) => (
          <ArtifactBlock key={`${item.kind}-${index}`} artifact={item} />
        ))}
        <details className="tool-block">
          <summary>工具结果 {formatToolSummary(message)}</summary>
          <pre>{formatJson(displayPayload)}</pre>
        </details>
      </div>
    )
  }
  if (message.messageType === 'tool-confirm') {
    return (
      <details className="tool-block">
        <summary>需要确认的工具调用</summary>
        <pre>{formatJson(message.payload?.toolFeedback ?? message.payload)}</pre>
      </details>
    )
  }
  return null
}

function ArtifactBlock({ artifact }: { artifact: Artifact }) {
  if (artifact.kind === 'file' && artifact.downloadUrl) {
    return (
      <div className="artifact-bar">
        <a className="artifact-link" href={artifact.downloadUrl} download={artifact.fileName}>
          下载 Excel{artifact.fileName ? ` · ${artifact.fileName}` : ''}
        </a>
        {artifact.rowCount != null ? <span>共 {artifact.rowCount} 行</span> : null}
      </div>
    )
  }
  if (artifact.kind === 'chart' && artifact.option) {
    return (
      <div className="chart-wrap">
        <ChartView option={artifact.option} />
      </div>
    )
  }
  return null
}

function MarkdownBody({ text }: { text: string }) {
  return (
    <div className="markdown">
      <Markdown remarkPlugins={[remarkGfm]}>{text}</Markdown>
    </div>
  )
}

const AGENT_TOOL_NAMES = new Set(['query_bank', 'summarize_bank', 'export_excel', 'create_chart'])
const DISPLAY_DATA_LIMIT = 2048

function toolNames(message: ChatMessage): string[] {
  const fromCalls = message.payload?.toolCalls?.map((item) => item.name).filter(Boolean) as string[]
  const fromResults = message.payload?.responses?.map((item) => item.name).filter(Boolean) as string[]
  return [...(fromCalls ?? []), ...(fromResults ?? [])]
}

/** Supervisor：· summarize_bank；子 Agent：· summarize_bank › summarizeTradeDetails */
function formatToolSummary(message: ChatMessage): string {
  const names = toolNames(message)
  if (!names.length) {
    return ''
  }
  const tools = names.join(', ')
  if (message.agentName && AGENT_TOOL_NAMES.has(message.agentName) && !AGENT_TOOL_NAMES.has(names[0])) {
    return `· ${message.agentName} › ${tools}`
  }
  return `· ${tools}`
}

/** 嵌套 tool 结果可能很长，展示时截断非 artifact 文本。 */
function truncateToolResponsesForDisplay(responses?: ToolResult[]): ToolResult[] | undefined {
  if (!responses?.length) {
    return responses
  }
  return responses.map((item) => {
    const raw = item.responseData
    if (!raw || raw.length <= DISPLAY_DATA_LIMIT) {
      return item
    }
    if (collectArtifacts([item]).length > 0) {
      return item
    }
    return { ...item, responseData: `${raw.slice(0, DISPLAY_DATA_LIMIT)}…` }
  })
}

function formatJson(value: unknown): string {
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

interface Artifact {
  kind: 'file' | 'chart'
  downloadUrl?: string
  fileName?: string
  rowCount?: number
  option?: Record<string, unknown>
}

function collectArtifacts(responses?: ToolResult[]): Artifact[] {
  if (!responses?.length) {
    return []
  }
  const artifacts: Artifact[] = []
  for (const item of responses) {
    artifacts.push(...parseArtifacts(item.responseData))
  }
  return artifacts
}

/**
 * 图表 / 下载类工具结果：界面上已单独渲染 artifact，工具块里只保留摘要，避免把整份 option JSON 塞进「工具结果」。
 */
function compactArtifactResponsesForDisplay(responses?: ToolResult[]): ToolResult[] | undefined {
  if (!responses?.length) {
    return responses
  }
  return responses.map((item) => {
    const artifacts = parseArtifacts(item.responseData)
    if (!artifacts.length) {
      return item
    }
    const parsed = item.responseData ? tryParseJson(item.responseData) : null
    const record =
      parsed && typeof parsed === 'object' && !Array.isArray(parsed)
        ? (parsed as Record<string, unknown>)
        : null
    const kinds = [...new Set(artifacts.map((artifact) => artifact.kind))]
    return {
      ...item,
      responseData: JSON.stringify({
        success: record && 'success' in record ? record.success : true,
        message: typeof record?.message === 'string' ? record.message : '产物已在上方展示',
        chartType: typeof record?.chartType === 'string' ? record.chartType : undefined,
        downloadUrl: typeof record?.downloadUrl === 'string' ? record.downloadUrl : undefined,
        fileName: typeof record?.fileName === 'string' ? record.fileName : undefined,
        rowCount: typeof record?.rowCount === 'number' ? record.rowCount : undefined,
        _artifacts: kinds,
      }),
    }
  })
}

/** 历史数据里 AgentTool 仍可能塞着整段自然语言；展示时压缩，避免工具块刷屏。 */
function compactToolResponsesForDisplay(responses?: ToolResult[]): ToolResult[] | undefined {
  if (!responses?.length) {
    return responses
  }
  return responses.map((item) => {
    if (!item.name || !AGENT_TOOL_NAMES.has(item.name)) {
      return item
    }
    const raw = item.responseData?.trim()
    if (!raw || !isAgentToolNarrative(raw)) {
      return item
    }
    const artifactJson = extractEmbeddedArtifactJson(raw)
    return {
      ...item,
      responseData:
        artifactJson ??
        JSON.stringify({
          _promoted: true,
          note: '子Agent回复已作为助手消息展示',
          chars: raw.length,
        }),
    }
  })
}

function isAgentToolNarrative(raw: string): boolean {
  if (raw.startsWith('{') || raw.startsWith('[')) {
    const parsed = tryParseJson(raw)
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      const record = parsed as Record<string, unknown>
      if (
        'success' in record ||
        'data' in record ||
        'downloadUrl' in record ||
        'option' in record ||
        '_promoted' in record
      ) {
        return false
      }
    } else if (parsed != null) {
      return false
    }
  }
  return true
}

function extractEmbeddedArtifactJson(raw: string): string | null {
  const parsed = tryParseJson(raw)
  if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
    const record = parsed as Record<string, unknown>
    if ('downloadUrl' in record || 'option' in record) {
      return JSON.stringify(parsed)
    }
  }
  return null
}

/**
 * 从工具返回的 JSON 文本中提取下载链接 / ECharts option。
 * 注意：JSON 解析失败时不得再把原字符串交给 artifactsFromUnknown，否则会与本函数互相递归。
 */
function parseArtifacts(raw?: string): Artifact[] {
  if (!raw) {
    return []
  }
  const parsed = tryParseJson(raw)
  if (parsed != null) {
    const fromObject = artifactsFromUnknown(parsed, 0)
    if (fromObject.length) {
      return fromObject
    }
  }
  return fileArtifactFromText(raw)
}

function tryParseJson(raw: string): unknown | null {
  try {
    return JSON.parse(raw)
  } catch {
    const start = raw.indexOf('{')
    const end = raw.lastIndexOf('}')
    if (start >= 0 && end > start) {
      try {
        return JSON.parse(raw.slice(start, end + 1))
      } catch {
        return null
      }
    }
    return null
  }
}

function fileArtifactFromText(raw: string): Artifact[] {
  const match = raw.match(/\/files\/[A-Za-z0-9-]+/)
  if (match) {
    return [{ kind: 'file', downloadUrl: match[0] }]
  }
  return []
}

function artifactsFromUnknown(value: unknown, depth: number): Artifact[] {
  if (depth > 6) {
    return []
  }
  if (typeof value === 'string') {
    // 只再解析一层 JSON；解析失败或仍是字符串时不再回调 parseArtifacts
    const nested = tryParseJson(value)
    if (nested != null && typeof nested !== 'string') {
      return artifactsFromUnknown(nested, depth + 1)
    }
    return fileArtifactFromText(value)
  }
  if (Array.isArray(value)) {
    return value.flatMap((item) => artifactsFromUnknown(item, depth + 1))
  }
  if (!value || typeof value !== 'object') {
    return []
  }
  const record = value as Record<string, unknown>
  const artifacts: Artifact[] = []
  if (typeof record.downloadUrl === 'string') {
    artifacts.push({
      kind: 'file',
      downloadUrl: record.downloadUrl,
      fileName: typeof record.fileName === 'string' ? record.fileName : undefined,
      rowCount: typeof record.rowCount === 'number' ? record.rowCount : undefined,
    })
  }
  if (record.option && typeof record.option === 'object' && !Array.isArray(record.option)) {
    artifacts.push({
      kind: 'chart',
      option: record.option as Record<string, unknown>,
    })
  }
  const nested = record.data
  if (nested != null && nested !== value) {
    artifacts.push(...artifactsFromUnknown(nested, depth + 1))
  }
  return artifacts
}
