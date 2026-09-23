import type { Reminder } from '../../api/types'
import { datePart, daysUntil, formatShortDate, type LocalDate, type LocalDateTime } from '../date'
import { sortBy } from './range'

export function pendingReminders(list: Reminder[]): Reminder[] {
  return sortBy(
    list.filter((r) => !r.sent),
    (r) => r.targetAt,
  )
}

/** "오늘 21:00" / "내일 09:00" / "어제 20:00" / "09.26" */
export function whenLabel(at: LocalDateTime, today: LocalDate): string {
  const d = daysUntil(datePart(at), today)
  const time = at.slice(11, 16)
  if (d === 0) return `오늘 ${time}`
  if (d === 1) return `내일 ${time}`
  if (d === -1) return `어제 ${time}`
  return formatShortDate(datePart(at))
}

export type Urgency = 'overdue' | 'soon' | 'near' | 'later'

/** 지남 / 24시간 안(오늘·내일) / 3일 안 / 그 뒤 */
export function urgency(at: LocalDateTime, now: LocalDateTime): Urgency {
  if (at < now) return 'overdue'
  const d = daysUntil(datePart(at), datePart(now))
  if (d <= 1) return 'soon'
  if (d <= 3) return 'near'
  return 'later'
}

/** targetDomain → 이동할 화면 */
export const DOMAIN_ROUTE: Record<string, { label: string; to: string }> = {
  pknu: { label: '학업', to: '/pknu' },
  study: { label: '공부', to: '/study' },
  health: { label: '건강', to: '/health' },
  life: { label: '생활', to: '/life' },
  schedule: { label: '일정', to: '/schedule' },
  memo: { label: '메모', to: '/memo' },
  hub: { label: '허브', to: '/hub' },
}
