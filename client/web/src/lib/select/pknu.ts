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
