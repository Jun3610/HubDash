import type { HabitLog, ReadingLog } from '../../api/types'
import { shiftDate, type LocalDate } from '../date'
import { sortBy } from './range'

export type CellState = 'done' | 'missed' | 'empty'

/** 트래커 칸 상태: 완료 / 지난 날 미완료(기록 없음 포함) / 오늘·미래인데 아직 기록 없음 */
export function cellState(logs: HabitLog[], date: LocalDate, today: LocalDate): CellState {
  const same = logs.filter((l) => l.performedAt === date)
  if (same.some((l) => l.completed)) return 'done'
  if (same.length > 0 || date < today) return 'missed'
  return 'empty'
}

/** 오늘까지 n일 (오래된 → 오늘) */
export function lastDays(today: LocalDate, n: number): LocalDate[] {
  return Array.from({ length: n }, (_, i) => shiftDate(today, i - n + 1))
}

/** 최근 7일 달성: 오늘 기록이 아직 없으면 분모에서 뺀다 (디자인의 "3/6") */
export function recentRate(logs: HabitLog[], today: LocalDate) {
  const days = lastDays(today, 7)
  const states = days.map((d) => cellState(logs, d, today))
  return { done: states.filter((x) => x === 'done').length, total: states.filter((x) => x !== 'empty').length }
}

export function readingState(b: ReadingLog): '읽는 중' | '완독' {
  return b.finishedAt ? '완독' : '읽는 중'
}

/** 읽는 중(최근 시작 순) 먼저, 그다음 완독(최근 완독 순) */
export function sortBooks(list: ReadingLog[]): ReadingLog[] {
  const reading = sortBy(
    list.filter((b) => !b.finishedAt),
    (b) => b.startedAt,
    'desc',
  )
  const done = sortBy(
    list.filter((b) => b.finishedAt),
    (b) => b.finishedAt as string,
    'desc',
  )
  return [...reading, ...done]
}

export function stars(n: number | null): string {
  if (!n) return '—'
  return '★'.repeat(n) + '☆'.repeat(5 - n)
}
