import { parseBlocks, renderInline } from '../../lib/markdown'
import { cx } from './cx'
import s from './Markdown.module.css'

/** 메모 미리보기. React 요소로만 그려서 HTML이 실행되지 않는다 */
export function Markdown({ source, className }: { source: string; className?: string }) {
  return (
    <div className={cx(s.md, className)}>
      {parseBlocks(source).map((b, i) => {
        switch (b.t) {
          case 'h': {
            const H = (['h2', 'h3', 'h4'] as const)[b.level - 1]
            return <H key={i}>{renderInline(b.text, `h${i}`)}</H>
          }
          case 'p':
            return <p key={i}>{renderInline(b.text, `p${i}`)}</p>
          case 'ul':
          case 'ol': {
            const L = b.t
            return (
              <L key={i}>
                {b.items.map((it, j) => (
                  <li key={j} data-task={it.checked !== undefined ? String(it.checked) : undefined}>
                    {it.checked !== undefined && (
                      <input
                        type="checkbox"
                        checked={it.checked}
                        readOnly
                        disabled
                        aria-label={it.checked ? '완료' : '미완료'}
                      />
                    )}
                    {renderInline(it.text, `l${i}-${j}`)}
                  </li>
                ))}
              </L>
            )
          }
          case 'quote':
            return <blockquote key={i}>{renderInline(b.text, `q${i}`)}</blockquote>
          case 'code':
            return (
              <pre key={i}>
                <code>{b.text}</code>
              </pre>
            )
          case 'hr':
            return <hr key={i} />
        }
      })}
    </div>
  )
}
