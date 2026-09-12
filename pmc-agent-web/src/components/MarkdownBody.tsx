import Markdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

export function MarkdownBody({ text }: { text: string }) {
  return (
    <div className="markdown">
      <Markdown remarkPlugins={[remarkGfm]}>{text}</Markdown>
    </div>
  )
}
