import { ExternalLink } from 'lucide-react'
import { browserUrl } from '../../lib/url'

/** 노션 필기 링크. 결정(이슈 #105)대로 브라우저 새 탭으로 연다 */
export function NotionLink({ url, label }: { url: string; label: string }) {
  return (
    <a
      href={browserUrl(url)}
      target="_blank"
      rel="noopener noreferrer"
      aria-label={label}
      title={label}
      style={{ display: 'inline-flex', verticalAlign: 'middle', color: 'var(--text-muted)', flexShrink: 0 }}
      onClick={(e) => e.stopPropagation()}
    >
      <ExternalLink size={13} strokeWidth={1.8} />
    </a>
  )
}
