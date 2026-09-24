import type { Reminder } from '../../api/types'
import { datePart, daysUntil, formatShortDate, shiftDate, type LocalDate, type LocalDateTime } from '../date'
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

export interface ReminderGroups {
  overdue: Reminder[] // 알림 시각이 지났는데 아직 안 보냄
  today: Reminder[]
  upcoming: Reminder[] // 내일 이후
  sent: Reminder[] // 최근 보낸 것부터
}

export function groupReminders(list: Reminder[], now: LocalDateTime): ReminderGroups {
  const today = datePart(now)
  const pending = pendingReminders(list)
  return {
    overdue: pending.filter((r) => r.targetAt < now),
    today: pending.filter((r) => r.targetAt >= now && datePart(r.targetAt) === today),
    upcoming: pending.filter((r) => datePart(r.targetAt) > today),
    sent: sortBy(
      list.filter((r) => r.sent),
      (r) => r.targetAt,
      'desc',
    ),
  }
}

/** 남은 시간: "30분 뒤" / "4시간 뒤" / 지났으면 "2시간 지남" */
export function untilLabel(at: LocalDateTime, now: LocalDateTime): string {
  const diff = Math.round((toMs(at) - toMs(now)) / 60000)
  const abs = Math.abs(diff)
  const text = abs < 60 ? `${abs}분` : abs < 60 * 24 ? `${Math.floor(abs / 60)}시간` : `${Math.floor(abs / 60 / 24)}일`
  return diff >= 0 ? `${text} 뒤` : `${text} 지남`
}

function toMs(t: LocalDateTime): number {
  return Date.UTC(+t.slice(0, 4), +t.slice(5, 7) - 1, +t.slice(8, 10), +t.slice(11, 13), +t.slice(14, 16))
}

function addMinutes(t: LocalDateTime, min: number): LocalDateTime {
  const d = new Date(toMs(t) + min * 60000)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getUTCFullYear()}-${p(d.getUTCMonth() + 1)}-${p(d.getUTCDate())}T${p(d.getUTCHours())}:${p(d.getUTCMinutes())}:00`
}

/** 빠른 선택: 1시간 뒤(분 단위 버림) / 오늘 밤 9시(이미 지났으면 내일) / 마감 하루 전 21:00 */
export function presetOneHour(now: LocalDateTime): LocalDateTime {
  return addMinutes(`${now.slice(0, 16)}:00`, 60)
}
export function presetTonight(now: LocalDateTime): LocalDateTime {
  const tonight = `${datePart(now)}T21:00:00`
  return tonight > now ? tonight : `${shiftDate(datePart(now), 1)}T21:00:00`
}
export function presetDayBefore(due: LocalDate): LocalDateTime {
  return `${shiftDate(due, -1)}T21:00:00`
}
/** 미루기 */
export function snooze(at: LocalDateTime, now: LocalDateTime, kind: '1h' | 'tomorrow'): LocalDateTime {
  const base = at > now ? at : `${now.slice(0, 16)}:00`
  return kind === '1h' ? addMinutes(base, 60) : `${shiftDate(datePart(base), 1)}${base.slice(10)}`
}
