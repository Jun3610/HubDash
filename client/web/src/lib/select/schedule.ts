import type { ScheduleEvent } from '../../api/types'
import { datePart, type LocalDate } from '../date'
import { overlapsDate, sortBy } from './range'

/** 그 날짜에 걸친 일정. 종일 일정 먼저, 나머지는 시작 시각 순 */
export function eventsOn(events: ScheduleEvent[], date: LocalDate): ScheduleEvent[] {
  const on = events.filter((e) => overlapsDate(e.startAt, e.endAt, date))
  return sortBy(on, (e) => (e.allDay ? '0' : '1') + e.startAt)
}

/** 목록 시각 표기: 종일 / 다른 날 시작이면 "이어짐" / 그 외 HH:mm */
export function eventTimeLabel(e: ScheduleEvent, date: LocalDate): string {
  if (e.allDay) return '종일'
  if (datePart(e.startAt) < date) return '이어짐'
  return e.startAt.slice(11, 16)
}
