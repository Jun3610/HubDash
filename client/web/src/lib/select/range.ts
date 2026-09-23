// 서버에 날짜 범위 필터가 없어 목록을 넉넉히 받아 프론트에서 거른다.
// 나중에 서버 필터가 생기면 이 파일의 함수들만 바꾸면 된다.
import { datePart, inRange, type LocalDate, type LocalDateTime } from '../date'

/** LocalDate 필드 기준 기간 필터 (양 끝 포함) */
export function withinDates<T>(items: T[], pick: (item: T) => LocalDate, from: LocalDate, to: LocalDate): T[] {
  return items.filter((item) => inRange(pick(item), from, to))
}

/** LocalDateTime 필드의 날짜 부분 기준 기간 필터 */
export function withinDateTimes<T>(items: T[], pick: (item: T) => LocalDateTime, from: LocalDate, to: LocalDate): T[] {
  return items.filter((item) => inRange(datePart(pick(item)), from, to))
}

/** 시작~끝이 걸쳐 있는 일정이 그 날짜에 보이는지 */
export function overlapsDate(startAt: LocalDateTime, endAt: LocalDateTime, date: LocalDate): boolean {
  return datePart(startAt) <= date && datePart(endAt) >= date
}

export function sortBy<T>(items: T[], key: (item: T) => string | number, dir: 'asc' | 'desc' = 'asc'): T[] {
  const sign = dir === 'asc' ? 1 : -1
  return [...items].sort((a, b) => {
    const ka = key(a)
    const kb = key(b)
    return ka < kb ? -sign : ka > kb ? sign : 0
  })
}
