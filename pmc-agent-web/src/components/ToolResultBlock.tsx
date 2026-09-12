import type { ToolMessage } from '@ag-ui/client'
import { collectArtifacts, displayToolPayload, type Artifact } from '../lib/artifacts'
import { agentNameOf, textContent } from '../lib/agui/message'
import { ChartView } from './ChartView'
import { formatToolSummary } from './ToolCallsBlock'

export function ToolResultBlock({ message, nested = false }: { message: ToolMessage; nested?: boolean }) {
  const raw = textContent(message)
  const artifacts = collectArtifacts(raw)
  const toolName = typeof message.metadata?.toolName === 'string' ? message.metadata.toolName : undefined
  return (
    <div className="tool-with-artifacts">
      {artifacts.map((item, index) => (
        <ArtifactBlock key={`${item.kind}-${index}`} artifact={item} />
      ))}
      <details className={`tool-block${nested ? ' nested' : ''}`}>
        <summary>工具结果 {formatToolSummary(agentNameOf(message), toolName ? [toolName] : [])}</summary>
        <pre>{displayToolPayload(raw)}</pre>
      </details>
    </div>
  )
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
