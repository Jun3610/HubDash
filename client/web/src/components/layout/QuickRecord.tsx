import { BookOpen, CalendarPlus, CheckSquare, FileText, Utensils } from 'lucide-react'
import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Modal } from '../ui'
import s from './Layout.module.css'
import { QuickContext } from './quickContext'

const ITEMS = [
  { to: '/health?new=meal', icon: Utensils, label: '끼니 추가', sub: '식단' },
  { to: '/study?new=1', icon: BookOpen, label: '공부 기록', sub: '분 단위' },
  { to: '/memo?tab=habits', icon: CheckSquare, label: '습관 체크', sub: '오늘' },
  { to: '/memo?new=1', icon: FileText, label: '메모 쓰기', sub: '마크다운' },
  { to: '/schedule?new=1', icon: CalendarPlus, label: '일정 추가', sub: '캘린더' },
]

export function QuickRecordProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false)

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
        setOpen(true)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open])

  return (
    <QuickContext.Provider value={() => setOpen(true)}>
      {children}
      <Modal open={open} onClose={() => setOpen(false)} title="빠른 기록 · ⌘↵">
        <div className={s.quickGrid}>
          {ITEMS.map((it) => (
            <Link key={it.to} to={it.to} className={s.quickItem} onClick={() => setOpen(false)}>
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
