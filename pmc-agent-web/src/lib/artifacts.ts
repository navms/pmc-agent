export interface Artifact {
  kind: 'file' | 'chart'
  downloadUrl?: string
  fileName?: string
  rowCount?: number
  option?: Record<string, unknown>
}

const DISPLAY_DATA_LIMIT = 2048

export function collectArtifacts(raw?: string): Artifact[] {
  if (!raw) {
    return []
  }
  return parseArtifacts(raw)
}

export function displayToolPayload(raw?: string): string {
  if (!raw) {
    return ''
  }
  const artifacts = parseArtifacts(raw)
  if (artifacts.length) {
    const parsed = tryParseJson(raw)
    const record =
      parsed && typeof parsed === 'object' && !Array.isArray(parsed)
        ? (parsed as Record<string, unknown>)
        : null
    const kinds = [...new Set(artifacts.map((item) => item.kind))]
    return JSON.stringify(
      {
        success: record && 'success' in record ? record.success : true,
        message: typeof record?.message === 'string' ? record.message : '产物已在上方展示',
        chartType: typeof record?.chartType === 'string' ? record.chartType : undefined,
        downloadUrl: typeof record?.downloadUrl === 'string' ? record.downloadUrl : undefined,
        fileName: typeof record?.fileName === 'string' ? record.fileName : undefined,
        rowCount: typeof record?.rowCount === 'number' ? record.rowCount : undefined,
        _artifacts: kinds,
      },
      null,
      2,
    )
  }
  if (raw.length > DISPLAY_DATA_LIMIT && artifacts.length === 0) {
    return `${raw.slice(0, DISPLAY_DATA_LIMIT)}…`
  }
  const parsed = tryParseJson(raw)
  if (parsed != null) {
    try {
      return JSON.stringify(parsed, null, 2)
    } catch {
      return raw
    }
  }
  return raw
}

function parseArtifacts(raw: string): Artifact[] {
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
