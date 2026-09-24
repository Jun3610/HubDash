import { Fragment, type ReactNode } from 'react'

// 메모 미리보기용 작은 마크다운 렌더러. HTML 문자열을 만들지 않고 React 요소로 그려서
// 메모에 <script> 같은 내용이 있어도 글자로만 보인다 (innerHTML을 쓰지 않음).
// 지원: # 제목(1–3), 목록(-, *, 1.), 체크박스(- [ ]), 인용(>), 코드 블록(```), 구분선(---), **굵게**, *기울임*, `코드`, [링크](http…)

type Block =
  | { t: 'h'; level: 1 | 2 | 3; text: string }
  | { t: 'p'; text: string }
  | { t: 'ul' | 'ol'; items: { text: string; checked?: boolean }[] }
  | { t: 'quote'; text: string }
  | { t: 'code'; text: string }
  | { t: 'hr' }

export function parseBlocks(src: string): Block[] {
  const lines = src.replace(/\r\n?/g, '\n').split('\n')
  const out: Block[] = []
  let i = 0
  while (i < lines.length) {
    const line = lines[i]
    if (line.startsWith('```')) {
      const body: string[] = []
      i++
      while (i < lines.length && !lines[i].startsWith('```')) body.push(lines[i++])
      i++ // 닫는 ``` (없으면 끝까지)
      out.push({ t: 'code', text: body.join('\n') })
      continue
    }
    const h = /^(#{1,3})\s+(.*)$/.exec(line)
    if (h) {
      out.push({ t: 'h', level: h[1].length as 1 | 2 | 3, text: h[2] })
      i++
      continue
    }
    if (/^\s*(-{3,}|\*{3,})\s*$/.test(line)) {
      out.push({ t: 'hr' })
      i++
      continue
    }
    if (/^\s*[-*]\s+/.test(line) || /^\s*\d+[.)]\s+/.test(line)) {
      const ordered = /^\s*\d+[.)]\s+/.test(line)
      const items: { text: string; checked?: boolean }[] = []
      const re = ordered ? /^\s*\d+[.)]\s+(.*)$/ : /^\s*[-*]\s+(.*)$/
      while (i < lines.length && re.test(lines[i])) {
        const text = re.exec(lines[i])![1]
        const task = /^\[( |x|X)\]\s+(.*)$/.exec(text)
        items.push(task ? { text: task[2], checked: task[1] !== ' ' } : { text })
        i++
      }
      out.push({ t: ordered ? 'ol' : 'ul', items })
      continue
    }
    if (line.startsWith('>')) {
      const body: string[] = []
      while (i < lines.length && lines[i].startsWith('>')) body.push(lines[i++].replace(/^>\s?/, ''))
      out.push({ t: 'quote', text: body.join('\n') })
      continue
    }
    if (!line.trim()) {
      i++
      continue
    }
    const para: string[] = []
    while (
      i < lines.length &&
      lines[i].trim() &&
      !/^(#{1,3}\s|```|>|\s*[-*]\s+|\s*\d+[.)]\s+|\s*(-{3,}|\*{3,})\s*$)/.test(lines[i])
    ) {
      para.push(lines[i++])
    }
    out.push({ t: 'p', text: para.join('\n') })
  }
  return out
}

const SAFE_URL = /^(https?:\/\/|mailto:)/i

/** 인라인: `코드` > [링크](url) > **굵게** > *기울임*. 줄바꿈은 <br> */
export function renderInline(text: string, keyPrefix = 'i'): ReactNode[] {
  const nodes: ReactNode[] = []
  const re = /(`[^`]+`)|\[([^\]]+)\]\(([^)\s]+)\)|(\*\*[^*]+\*\*)|(\*[^*\s][^*]*\*)|(\n)/g
  let last = 0
  let m: RegExpExecArray | null
  let k = 0
  while ((m = re.exec(text))) {
    if (m.index > last) nodes.push(text.slice(last, m.index))
    const key = `${keyPrefix}-${k++}`
    if (m[1]) nodes.push(<code key={key}>{m[1].slice(1, -1)}</code>)
    else if (m[2]) {
      nodes.push(
        SAFE_URL.test(m[3]) ? (
          <a key={key} href={m[3]} target="_blank" rel="noopener noreferrer">
            {m[2]}
          </a>
        ) : (
          // 허용하지 않는 주소(javascript: 등)는 링크로 만들지 않는다
          <Fragment key={key}>{m[0]}</Fragment>
        ),
      )
    } else if (m[4]) nodes.push(<strong key={key}>{renderInline(m[4].slice(2, -2), key)}</strong>)
    else if (m[5]) nodes.push(<em key={key}>{m[5].slice(1, -1)}</em>)
    else if (m[6]) nodes.push(<br key={key} />)
    last = re.lastIndex
  }
  if (last < text.length) nodes.push(text.slice(last))
  return nodes
}

/** 목록 미리보기 한 줄: 마크다운 기호를 걷어낸 첫 내용 */
export function plainSnippet(src: string, max = 80): string {
  const text = src
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/^#{1,3}\s+/gm, '')
    .replace(/^\s*([-*]|\d+[.)])\s+(\[[ xX]\]\s+)?/gm, '')
    .replace(/^>\s?/gm, '')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/[*`]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  return text.length > max ? `${text.slice(0, max)}…` : text
}
