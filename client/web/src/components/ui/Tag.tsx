import type { ReactNode } from 'react'
import { daysUntil, ddayLabel, ddayTone, type LocalDate } from '../../lib/date'
import s from './Tag.module.css'
import type { Tone } from './tone'
import { cx } from './cx'

/** 테두리 없이 옅은 배경 + 같은 계열 글자 */
export function Tag({
  tone = 'neutral',
  size = 'sm',
  mono,
  children,
  title,
}: {
  tone?: Tone
  size?: 'sm' | 'lg'
  mono?: boolean
  children: ReactNode
  title?: string
}) {
  return (
    <span className={cx(s.tag, s[tone], size === 'lg' && s.lg, mono && s.mono)} title={title}>
      {children}
    </span>
  )
}

/** 탭·메뉴 옆 숫자 알약 */
export function Counter({ children, accent }: { children: ReactNode; accent?: boolean }) {
  return <span className={cx(s.counter, accent && s.counterAccent)}>{children}</span>
}

/** D-1 이하 빨강 · D-3 이하 노랑 · 그 외 회색 · 완료 보라 */
export function DdayBadge({ due, today, completed }: { due: LocalDate; today: LocalDate; completed?: boolean }) {
  const days = daysUntil(due, today)
  const tone = ddayTone(days, completed)
  return (
    <Tag tone={tone} mono title={due}>
      {completed ? '완료' : ddayLabel(days)}
    </Tag>
  )
}
