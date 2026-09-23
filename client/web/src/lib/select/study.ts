import type { StudyProgress } from '../../api/types'
import { shiftDate, weekStartOf, type LocalDate } from '../date'
import { currentStreak } from '../heatmap'

/** 최근 n주(이번 주 포함, 오래된 → 최근) 주별 공부 분 */
export function weeklyMinutes(
  list: StudyProgress[],
  today: LocalDate,
  n = 8,
): { weekStart: LocalDate; minutes: number }[] {
  const thisWeek = weekStartOf(today)
  const weeks = Array.from({ length: n }, (_, i) => shiftDate(thisWeek, -7 * (n - 1 - i)))
  const sums = new Map(weeks.map((w) => [w, 0]))
  for (const p of list) {
    const w = weekStartOf(p.studiedAt)
    if (sums.has(w)) sums.set(w, sums.get(w)! + p.minutes)
  }
  return weeks.map((w) => ({ weekStart: w, minutes: sums.get(w)! }))
}

/** 기록이 있는 날짜 집합에서 가장 긴 연속 일수 */
export function longestStreak(dates: LocalDate[]): number {
  const sorted = [...new Set(dates)].sort()
  let best = 0
  let run = 0
  let prev: LocalDate | null = null
  for (const d of sorted) {
    run = prev && shiftDate(prev, 1) === d ? run + 1 : 1
    best = Math.max(best, run)
    prev = d
  }
  return best
}

export function studyStreak(list: StudyProgress[], today: LocalDate): number {
  const counts = new Map<LocalDate, number>()
  for (const p of list) counts.set(p.studiedAt, 1)
  return currentStreak(counts, today)
}
