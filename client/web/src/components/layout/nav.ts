import {
  BookOpen,
  CalendarDays,
  FileText,
  GraduationCap,
  Heart,
  House,
  Link2,
  Settings,
  type LucideIcon,
} from 'lucide-react'

export type NavKey = 'home' | 'hub' | 'study' | 'pknu' | 'health' | 'schedule' | 'memo' | 'settings'

export interface NavItem {
  key: NavKey
  label: string
  /** 브레드크럼용 짧은 이름 */
  crumb: string
  to: string
  icon: LucideIcon
}

export const NAV: NavItem[] = [
  { key: 'home', label: '홈', crumb: '홈', to: '/', icon: House },
  { key: 'hub', label: '허브', crumb: '허브', to: '/hub', icon: Link2 },
  { key: 'study', label: '공부', crumb: '공부', to: '/study', icon: BookOpen },
  { key: 'pknu', label: '학업 · PKNU', crumb: '학업 · PKNU', to: '/pknu', icon: GraduationCap },
  { key: 'health', label: '건강', crumb: '건강', to: '/health', icon: Heart },
  { key: 'schedule', label: '일정', crumb: '일정', to: '/schedule', icon: CalendarDays },
  { key: 'memo', label: '메모 · 습관', crumb: '메모', to: '/memo', icon: FileText },
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
