import type { Assignment } from '../../api/types'
import { inRange, shiftDate, weekStartOf, type LocalDate } from '../date'
import { sortBy } from './range'

/**
 * 홈 과제 카드: 미완료(마감 순, 지난 것 포함) 먼저, 남는 자리는 최근 7일 안에 마감된 완료 과제로 채운다
 */
export function homeAssignments(list: Assignment[], today: LocalDate, limit = 4): Assignment[] {
  const open = sortBy(
    list.filter((a) => !a.completed),
    (a) => a.dueDate,
  )
  const recentDone = sortBy(
    list.filter((a) => a.completed && inRange(a.dueDate, shiftDate(today, -7), shiftDate(today, 7))),
    (a) => a.dueDate,
    'desc',
  )
  return [...open, ...recentDone].slice(0, limit)
}

/** 이번 주(월~일) 마감 과제의 완료율 */
export function weekCompletion(list: Assignment[], today: LocalDate) {
  const from = weekStartOf(today)
  const to = shiftDate(from, 6)
  const week = list.filter((a) => inRange(a.dueDate, from, to))
  const done = week.filter((a) => a.completed).length
  return { done, total: week.length, rate: week.length ? done / week.length : 0 }
}

export type SemesterStatus = '진행 중' | '예정' | '종료'

export function semesterStatus(start: LocalDate, end: LocalDate, today: LocalDate): SemesterStatus {
  if (today < start) return '예정'
  if (today > end) return '종료'
  return '진행 중'
}

/** 개강 주(월요일 기준)를 1주차로 센 현재 주차. 기간 밖이면 null */
export function weekNumber(start: LocalDate, end: LocalDate, today: LocalDate): number | null {
  if (today < start || today > end) return null
  const diff = daysBetween(weekStartOf(start), weekStartOf(today))
  return Math.floor(diff / 7) + 1
}

function daysBetween(a: LocalDate, b: LocalDate): number {
  return Math.round(
    (Date.UTC(+b.slice(0, 4), +b.slice(5, 7) - 1, +b.slice(8, 10)) -
      Date.UTC(+a.slice(0, 4), +a.slice(5, 7) - 1, +a.slice(8, 10))) /
      86_400_000,
  )
}

/** 과목별 과제 진행: 완료/전체, 오늘 이후 가장 가까운 미완료 마감 */
export function courseProgress(list: Assignment[], today: LocalDate) {
  const done = list.filter((a) => a.completed).length
  const next = sortBy(
    list.filter((a) => !a.completed && a.dueDate >= today),
    (a) => a.dueDate,
  )[0]
  const overdue = list.filter((a) => !a.completed && a.dueDate < today).length
  return { done, total: list.length, next, overdue }
}

/** 과목별 주간 통계를 주 단위로 합친다 (최근 n주, 오래된 → 최근) */
export function mergeWeekly(
  rows: { weekStart: LocalDate; totalCount: number; completedCount: number }[],
  n = 4,
): { weekStart: LocalDate; total: number; done: number; rate: number }[] {
  const byWeek = new Map<LocalDate, { total: number; done: number }>()
  for (const r of rows) {
    const cur = byWeek.get(r.weekStart) ?? { total: 0, done: 0 }
    cur.total += r.totalCount
    cur.done += r.completedCount
    byWeek.set(r.weekStart, cur)
  }
  return [...byWeek.entries()]
    .sort(([a], [b]) => (a < b ? -1 : 1))
    .slice(-n)
    .map(([weekStart, v]) => ({ weekStart, ...v, rate: v.total ? v.done / v.total : 0 }))
}
