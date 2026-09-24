import { CalendarDays, FileText, Link2, Search } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate } from 'react-router-dom'
import { memos } from '../../api/memo'
import { BIG_PAGE, useList } from '../../api/resource'
import { useAllEvents } from '../../hooks/useEvents'
import { hostOf, useAllHubLinks } from '../../hooks/useHub'
import { datePart } from '../../lib/date'
import { searchEvents, timeRange } from '../../lib/select/schedule'
import { browserUrl } from '../../lib/url'
import s from './Search.module.css'

interface Hit {
  key: string
  group: '메모' | '일정' | '허브 링크'
  title: string
  meta: string
  run: () => void
}

/** 전역 검색: 메모 제목, 일정(#160), 허브 링크 */
export function SearchPalette({ open, onClose }: { open: boolean; onClose: () => void }) {
  // 열 때마다 새로 마운트해 검색어·선택을 초기화한다
  return open ? <Palette onClose={onClose} /> : null
}

function Palette({ onClose }: { onClose: () => void }) {
  const [q, setQ] = useState('')
  const [sel, setSel] = useState(0)
  const inputRef = useRef<HTMLInputElement>(null)
  const navigate = useNavigate()
  const memoList = useList(memos, { size: BIG_PAGE, sort: 'updatedAt,desc' })
  const hub = useAllHubLinks()
  const eventList = useAllEvents()

  useEffect(() => inputRef.current?.focus(), [])

  const hits = useMemo<Hit[]>(() => {
    const needle = q.trim().toLowerCase()
    const match = (t: string | null | undefined) => !needle || (t ?? '').toLowerCase().includes(needle)
    const go = (to: string) => () => {
      navigate(to)
      onClose()
    }
    const out: Hit[] = []
    for (const m of memoList.data?.content ?? []) {
      if (match(m.title) || (needle && match(m.tags)))
        out.push({
          key: `m${m.id}`,
          group: '메모',
          title: m.title,
          meta: m.updatedAt.slice(5, 10).replace('-', '.'),
          run: go(`/memo?id=${m.id}`),
        })
    }
    // 일정은 검색어가 있을 때만 (300건이 넘어서)
    for (const e of searchEvents(eventList.data?.content ?? [], needle)) {
      const d = datePart(e.startAt)
      out.push({
        key: `e${e.id}`,
        group: '일정',
        title: e.title,
        meta: `${d.slice(2).replaceAll('-', '.')} ${e.allDay ? 'All day' : timeRange(e)}`,
        run: go(`/schedule?date=${d}&event=${e.id}`),
      })
    }
    for (const l of hub.links) {
      if (match(l.title) || (needle && match(l.url)))
        out.push({
          key: `l${l.id}`,
          group: '허브 링크',
          title: l.title,
          meta: hostOf(l.url),
          run: () => {
            window.open(browserUrl(l.url), '_blank', 'noopener')
            onClose()
          },
        })
    }
    // 그룹별 최대 8개
    const limited: Hit[] = []
    for (const g of ['메모', '일정', '허브 링크'] as const)
      limited.push(...out.filter((h) => h.group === g).slice(0, 8))
    return limited
  }, [q, memoList.data, eventList.data, hub.links, navigate, onClose])

  const onKey = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') onClose()
    else if (e.key === 'ArrowDown') {
      e.preventDefault()
      setSel((i) => Math.min(hits.length - 1, i + 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setSel((i) => Math.max(0, i - 1))
    } else if (e.key === 'Enter') {
      hits[sel]?.run()
    }
  }

  const ICON = { 메모: FileText, 일정: CalendarDays, '허브 링크': Link2 }
  // 그룹 이름은 영어로 보여 준다 (이슈 #209)
  const GROUP_LABEL = { 메모: 'Memos', 일정: 'Schedule', '허브 링크': 'Hub Links' }
  let lastGroup = ''

  return createPortal(
    <div className={s.overlay} onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className={s.box} role="dialog" aria-modal="true" aria-label="전역 검색" onKeyDown={onKey}>
        <div className={s.inputRow}>
          <Search size={15} />
          <input
            ref={inputRef}
            className={s.input}
            placeholder="Search memos, schedules, hub links"
            value={q}
            onChange={(e) => {
              setQ(e.target.value)
              setSel(0)
            }}
            role="combobox"
            aria-expanded="true"
            aria-controls="search-results"
            aria-activedescendant={hits[sel] ? `hit-${hits[sel].key}` : undefined}
          />
        </div>
        <ul id="search-results" role="listbox" className={s.list}>
          {hits.length === 0 && <li className={s.empty}>No results</li>}
          {hits.map((h, i) => {
            const header = h.group !== lastGroup
            lastGroup = h.group
            const Icon = ICON[h.group]
            return (
              <li key={h.key} role="presentation">
                {header && <div className={s.group}>{GROUP_LABEL[h.group]}</div>}
                <div
                  id={`hit-${h.key}`}
                  role="option"
                  aria-selected={i === sel}
                  className={s.item}
                  onMouseEnter={() => setSel(i)}
                  onClick={h.run}
                >
                  <Icon size={14} strokeWidth={1.7} color="var(--text-muted)" />
                  <span className="ellipsis">{h.title}</span>
                  <span className={s.meta}>{h.meta}</span>
                </div>
              </li>
            )
          })}
        </ul>
        <div className={s.foot}>
          <span>↑↓ Move</span>
          <span>Enter Open</span>
          <span>Esc Close</span>
        </div>
      </div>
    </div>,
    document.body,
  )
}
