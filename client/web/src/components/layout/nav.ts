import { BookOpen, CalendarDays, FileText, GraduationCap, Heart, House, Settings, type LucideIcon } from 'lucide-react'

export type NavKey = 'home' | 'study' | 'pknu' | 'health' | 'schedule' | 'memo' | 'settings'

export interface NavItem {
  key: NavKey
  label: string
  /** 브레드크럼용 짧은 이름 */
  crumb: string
  to: string
  icon: LucideIcon
}

export const NAV: NavItem[] = [
  { key: 'home', label: 'Home', crumb: 'Home', to: '/', icon: House },
  { key: 'study', label: 'Study', crumb: 'Study', to: '/study', icon: BookOpen },
  { key: 'pknu', label: 'PKNU', crumb: 'PKNU', to: '/pknu', icon: GraduationCap },
  { key: 'health', label: 'Health', crumb: 'Health', to: '/health', icon: Heart },
  { key: 'schedule', label: 'Schedule', crumb: 'Schedule', to: '/schedule', icon: CalendarDays },
  { key: 'memo', label: 'Memo', crumb: 'Memo', to: '/memo', icon: FileText },
]

/** 저장된 순서대로 정렬. 순서에 없는 메뉴(새로 생긴 것)는 뒤에 원래 순서로 붙인다 */
export function orderedNav(order: readonly string[]): NavItem[] {
  const rank = (k: string) => {
    const i = order.indexOf(k)
    return i === -1 ? order.length + NAV.findIndex((n) => n.key === k) : i
  }
  return [...NAV].sort((a, b) => rank(a.key) - rank(b.key))
}

export const SETTINGS_NAV: NavItem = { key: 'settings', label: '설정', crumb: '설정', to: '/settings', icon: Settings }
