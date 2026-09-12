export async function* readSse(
  response: Response,
): AsyncGenerator<{ event?: string; data: string }> {
  if (!response.body) {
    throw new Error('响应没有数据流')
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let event: string | undefined
  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split('\n')
    buffer = parts.pop() ?? ''
    for (const raw of parts) {
      const line = raw.replace(/\r$/, '')
      if (line.startsWith('event:')) {
        event = line.slice(6).trim()
        continue
      }
      if (line.startsWith('data:')) {
        yield { event, data: line.slice(5).trim() }
        event = undefined
      }
    }
  }
}
