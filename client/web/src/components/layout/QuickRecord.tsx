import { Bell, BookOpen, CalendarPlus, CheckSquare, FileText, Utensils } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Modal } from '../ui'
import s from './Layout.module.css'
import { QuickContext } from './quickContext'

const ITEMS = [
  { to: '/health?new=meal', icon: Utensils, label: '끼니 추가', sub: '식단' },
  { to: '/study?new=1', icon: BookOpen, label: '공부 기록', sub: '분 단위' },
  { to: '/life', icon: CheckSquare, label: '습관 체크', sub: '오늘' },
  { to: '/memo?new=1', icon: FileText, label: '메모 쓰기', sub: '마크다운' },
  { to: '/schedule?new=1', icon: CalendarPlus, label: '일정 추가', sub: '캘린더' },
  { to: '/reminders?new=1', icon: Bell, label: '리마인더', sub: '알림' },
]

export function QuickRecordProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false)
  return (
    <QuickContext.Provider value={() => setOpen(true)}>
      {children}
      <Modal open={open} onClose={() => setOpen(false)} title="빠른 기록">
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
