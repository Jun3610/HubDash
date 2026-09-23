import type { ReactNode } from 'react'
import { Counter } from './Tag'
import s from './Tabs.module.css'
import { cx } from './cx'

export interface TabItem<K extends string> {
  key: K
  label: ReactNode
  count?: number
}

/** 밑줄형 탭. 현재 탭은 굵게 + 2px 강조색 밑줄 + 카운터 알약 */
export function Tabs<K extends string>({
  items,
  value,
  onChange,
  inHeader,
  label,
}: {
  items: TabItem<K>[]
  value: K
  onChange: (key: K) => void
  inHeader?: boolean
  label: string
}) {
  return (
    <div role="tablist" aria-label={label} className={cx(s.tabs, inHeader && s.inHeader)}>
      {items.map((it) => {
        const on = it.key === value
        return (
          <button
            key={it.key}
            type="button"
            role="tab"
            aria-selected={on}
            className={cx(s.tab, on && s.active)}
            onClick={() => onChange(it.key)}
          >
            {it.label}
            {it.count !== undefined && <Counter>{it.count}</Counter>}
          </button>
        )
      })}
    </div>
  )
}

/** 작은 전환 버튼 묶음 (카드/목록, 월/주/목록 등) */
export function Segmented<K extends string>({
  items,
  value,
  onChange,
  label,
}: {
  items: { key: K; label: ReactNode; title?: string }[]
  value: K
  onChange: (key: K) => void
  label: string
}) {
  return (
    <div role="radiogroup" aria-label={label} className={s.seg}>
      {items.map((it) => (
        <button
          key={it.key}
          type="button"
          role="radio"
          aria-checked={it.key === value}
          title={it.title}
          className={cx(s.segBtn, it.key === value && s.segOn)}
          onClick={() => onChange(it.key)}
        >
          {it.label}
        </button>
      ))}
    </div>
  )
}
