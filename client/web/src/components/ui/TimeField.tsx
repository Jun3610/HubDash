import { Clock } from 'lucide-react'
import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { cx } from './cx'
import s from './TimeField.module.css'

const pad = (n: number) => String(n).padStart(2, '0')
const HOURS = Array.from({ length: 24 }, (_, i) => i)

/**
 * macOS 스타일 시간 선택 (이슈 #148). 누르면 시·분 두 줄 목록이 펼쳐지고,
 * 닫힌 상태에서도 ↑↓로 분을 minuteStep만큼 옮길 수 있다. 값은 "HH:mm".
 */
export function TimeField({
  value,
  onChange,
  minuteStep = 5,
  id,
  'aria-invalid': invalid,
  'aria-describedby': describedBy,
  'aria-label': ariaLabel,
}: {
  value: string
  onChange: (v: string) => void
  minuteStep?: number
  id?: string
  'aria-invalid'?: boolean
  'aria-describedby'?: string
  'aria-label'?: string
  required?: boolean
}) {
  const [open, setOpen] = useState(false)
  const root = useRef<HTMLDivElement>(null)
  const [h, m] = (/^\d{2}:\d{2}$/.test(value) ? value : '12:00').split(':').map(Number)
  const minutes = Array.from({ length: 60 / minuteStep }, (_, i) => i * minuteStep)
  // 5분 단위가 아닌 기존 값(예: 12:07)도 목록에 넣어 둔다
  if (!minutes.includes(m)) {
    minutes.push(m)
    minutes.sort((a, b) => a - b)
  }

  const set = (hh: number, mm: number) => onChange(`${pad(hh)}:${pad(mm)}`)
  const shift = (delta: number) => {
    const total = (((h * 60 + m + delta) % 1440) + 1440) % 1440
    set(Math.floor(total / 60), total % 60)
  }

  // 바깥을 누르면 닫는다
  useEffect(() => {
    if (!open) return
    const onDown = (e: MouseEvent) => {
      if (!root.current?.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onDown)
    // 고른 값이 목록 가운데 오게
    root.current
      ?.querySelectorAll<HTMLElement>('[aria-selected="true"]')
      .forEach((el) => el.scrollIntoView({ block: 'center' }))
    return () => document.removeEventListener('mousedown', onDown)
  }, [open])

  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
      e.preventDefault()
      shift((e.key === 'ArrowUp' ? 1 : -1) * minuteStep)
    } else if (e.key === 'Escape' && open) {
      // 모달까지 닫히지 않게 여기서 멈춘다
      e.stopPropagation()
      e.nativeEvent.stopImmediatePropagation()
      setOpen(false)
    }
  }

  return (
    <div ref={root} className={s.root} onKeyDown={onKey}>
      <button
        type="button"
        id={id}
        className={cx(s.trigger, open && s.open)}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-invalid={invalid}
        aria-describedby={describedBy}
        aria-label={ariaLabel ? `${ariaLabel} ${pad(h)}시 ${pad(m)}분` : undefined}
        onClick={() => setOpen((v) => !v)}
      >
        <span className={s.value}>
          <span>{pad(h)}</span>
          <span className={s.colon}>:</span>
          <span>{pad(m)}</span>
        </span>
        <span className={s.ampm}>{h < 12 ? '오전' : '오후'}</span>
        <Clock size={14} strokeWidth={1.8} className={s.icon} aria-hidden="true" />
      </button>
      {open && (
        <div className={s.pop}>
          <Column label="시" items={HOURS} selected={h} onPick={(v) => set(v, m)} />
          <Column label="분" items={minutes} selected={m} onPick={(v) => set(h, v)} />
        </div>
      )}
    </div>
  )
}

function Column({
  label,
  items,
  selected,
  onPick,
}: {
  label: string
  items: number[]
  selected: number
  onPick: (v: number) => void
}) {
  return (
    <div className={s.col}>
      <span className={s.colHead}>{label}</span>
      <div role="listbox" aria-label={label} className={s.list}>
        {items.map((v) => (
          <button
            key={v}
            type="button"
            role="option"
            aria-selected={v === selected}
            className={s.opt}
            onClick={() => onPick(v)}
          >
            {pad(v)}
          </button>
        ))}
      </div>
    </div>
  )
}
