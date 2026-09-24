import { BookOpen, CalendarPlus, CheckSquare, FileText, Utensils } from 'lucide-react'
import { useEffect, useRef, useState, type KeyboardEvent as ReactKeyboardEvent, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Modal } from '../ui'
import s from './Layout.module.css'
import { QuickContext } from './quickContext'

const ITEMS = [
  { to: '/health?new=meal', icon: Utensils, label: 'Add Meal', sub: 'Diet' },
  { to: '/study?new=1', icon: BookOpen, label: 'Log Study', sub: 'Minutes' },
  { to: '/memo?tab=habits', icon: CheckSquare, label: 'Check Habits', sub: 'Today' },
  { to: '/memo?new=1', icon: FileText, label: 'Write Memo', sub: 'Markdown' },
  { to: '/schedule?new=1', icon: CalendarPlus, label: 'Add Schedule', sub: 'Calendar' },
]

export function QuickRecordProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false)
  // 방향키로 옮기고 Enter로 고른다 (이슈 #193). 2열이라 ←→는 한 칸, ↑↓는 한 줄
  const [sel, setSel] = useState(0)
  const links = useRef<(HTMLAnchorElement | null)[]>([])
  const navigate = useNavigate()
  const COLS = 2
  const move = (i: number) => {
    const next = (i + ITEMS.length) % ITEMS.length
    setSel(next)
    links.current[next]?.focus()
  }
  const onGridKey = (e: ReactKeyboardEvent) => {
    const step = { ArrowRight: 1, ArrowLeft: -1, ArrowDown: COLS, ArrowUp: -COLS }[e.key]
    if (step !== undefined) {
      e.preventDefault()
      move(sel + step)
    } else if (e.key === 'Enter' && !e.metaKey && !e.ctrlKey) {
      e.preventDefault()
      setOpen(false)
      navigate(ITEMS[sel].to)
    }
  }
  // 열릴 때 첫 항목에 포커스 (고른 항목은 여는 쪽에서 0으로 맞춘다)
  useEffect(() => {
    if (!open) return
    const t = setTimeout(() => links.current[0]?.focus(), 0)
    return () => clearTimeout(t)
  }, [open])
  const show = () => {
    setSel(0)
    setOpen(true)
  }

  // 어디서든 ⌘/Ctrl + Enter로 연다 (이슈 #187). 다른 창이 떠 있으면 그 작업을 방해하지 않게 두고
  // 빠른 기록 창이 이미 열려 있으면 닫는다
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (!(e.metaKey || e.ctrlKey) || e.key !== 'Enter' || e.isComposing) return
      const dialogs = document.querySelectorAll('[role="dialog"]').length
      if (open) {
        e.preventDefault()
        setOpen(false)
      } else if (dialogs === 0) {
        e.preventDefault()
        show()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open])

  return (
    <QuickContext.Provider value={show}>
      {children}
      <Modal open={open} onClose={() => setOpen(false)} title="Quick Log · ⌘↵">
        <div className={s.quickGrid} role="listbox" aria-label="빠른 기록 항목" onKeyDown={onGridKey}>
          {ITEMS.map((it, i) => (
            <Link
              key={it.to}
              ref={(el) => {
                links.current[i] = el
              }}
              to={it.to}
              role="option"
              aria-selected={i === sel}
              className={s.quickItem}
              onMouseEnter={() => setSel(i)}
              onFocus={() => setSel(i)}
              onClick={() => setOpen(false)}
            >
              <it.icon size={16} strokeWidth={1.7} />
              <span>
                {it.label}
                <small>{it.sub}</small>
              </span>
            </Link>
          ))}
        </div>
      </Modal>
    </QuickContext.Provider>
  )
}
