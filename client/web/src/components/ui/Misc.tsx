import { ChevronLeft, ChevronRight, Pencil, Trash2 } from 'lucide-react'
import type { ReactNode, TableHTMLAttributes } from 'react'
import { formatHeaderDate, shiftDate, type LocalDate } from '../../lib/date'
import { IconButton } from './Button'
import s from './Misc.module.css'
import { cx } from './cx'

/** 줄에 마우스를 올리면 보이는 수정·삭제 버튼. 부모 줄에 className="hover-row" */
export function RowActions({ onEdit, onDelete, label }: { onEdit?: () => void; onDelete?: () => void; label: string }) {
  return (
    <span className={s.rowActions}>
      {onEdit && (
        <IconButton label={`${label} 수정`} size="sm" onClick={onEdit}>
          <Pencil size={13} strokeWidth={1.7} />
        </IconButton>
      )}
      {onDelete && (
        <IconButton label={`${label} 삭제`} size="sm" onClick={onDelete}>
          <Trash2 size={13} strokeWidth={1.7} />
        </IconButton>
      )}
    </span>
  )
}

/** ◀ 2026.09.23 수 ▶ (+ 오늘) */
export function DateNav({
  value,
  onChange,
  today,
  step = 1,
  label,
}: {
  value: LocalDate
  onChange: (d: LocalDate) => void
  today: LocalDate
  step?: number
  label?: ReactNode
}) {
  return (
    <div className={s.dateNav}>
      <button type="button" className={s.navBtn} aria-label="이전" onClick={() => onChange(shiftDate(value, -step))}>
        <ChevronLeft size={14} />
      </button>
      <span className={s.dateLabel} aria-live="polite">
        {label ?? formatHeaderDate(value)}
      </span>
      <button type="button" className={s.navBtn} aria-label="다음" onClick={() => onChange(shiftDate(value, step))}>
        <ChevronRight size={14} />
      </button>
      {value !== today && (
        <button type="button" className={s.todayBtn} onClick={() => onChange(today)}>
          오늘
        </button>
      )}
    </div>
  )
}

export function Table({ className, ...rest }: TableHTMLAttributes<HTMLTableElement>) {
  return <table className={cx(s.table, className)} {...rest} />
}
