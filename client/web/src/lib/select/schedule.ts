import type { ScheduleEvent } from '../../api/types'
import { datePart, shiftDate, weekStartOf, type LocalDate } from '../date'
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

export const GRID_START_HOUR = 8
export const GRID_END_HOUR = 22

function minutesOf(t: string): number {
  return Number(t.slice(11, 13)) * 60 + Number(t.slice(14, 16))
}

export interface PlacedEvent {
  event: ScheduleEvent
  /** 격자 시작(08:00)부터의 분 */
  top: number
  height: number
  lane: number
  lanes: number
  /** 격자 밖으로 잘렸는지 */
  clippedTop: boolean
  clippedBottom: boolean
}

/**
 * 주 보기 한 칸(하루)에 시간 일정 배치. 여러 날에 걸친 일정은 그날 부분만, 격자(08–22시) 밖은 잘라서 가장자리에 붙인다.
 * 겹치는 일정은 나란히 놓도록 줄(lane)을 나눈다.
 */
export function placeDay(events: ScheduleEvent[], date: LocalDate): PlacedEvent[] {
  const gridStart = GRID_START_HOUR * 60
  const gridEnd = GRID_END_HOUR * 60
  const items = events
    .filter((e) => !e.allDay && overlapsDate(e.startAt, e.endAt, date))
    .map((e) => {
      const s = datePart(e.startAt) < date ? 0 : minutesOf(e.startAt)
      const en = datePart(e.endAt) > date ? 24 * 60 : minutesOf(e.endAt)
      const start = Math.min(Math.max(s, gridStart), gridEnd - 15)
      const end = Math.max(Math.min(en, gridEnd), start + 15)
      return { event: e, start, end, clippedTop: s < gridStart, clippedBottom: en > gridEnd }
    })
    .sort((a, b) => a.start - b.start || b.end - a.end)

  // 겹치는 무리(cluster)마다 줄 수를 정한다
  const placed: PlacedEvent[] = []
  let cluster: ((typeof items)[number] & { lane: number })[] = []
  let clusterEnd = -1
  const flush = () => {
    const lanes = Math.max(1, ...cluster.map((c) => c.lane + 1))
    for (const c of cluster) {
      placed.push({
        event: c.event,
        top: c.start - gridStart,
        height: c.end - c.start,
        lane: c.lane,
        lanes,
        clippedTop: c.clippedTop,
        clippedBottom: c.clippedBottom,
      })
    }
    cluster = []
  }
  for (const it of items) {
    if (it.start >= clusterEnd && cluster.length) flush()
    const laneEnds: number[] = []
    for (const c of cluster) laneEnds[c.lane] = Math.max(laneEnds[c.lane] ?? -1, c.end)
    let lane = laneEnds.findIndex((end) => end <= it.start)
    if (lane === -1) lane = laneEnds.length
    cluster.push({ ...it, lane })
    clusterEnd = Math.max(clusterEnd, it.end)
  }
  if (cluster.length) flush()
  return placed
}

/** 그날 걸친 종일 일정 */
export function allDayOn(events: ScheduleEvent[], date: LocalDate): ScheduleEvent[] {
  return events.filter((e) => e.allDay && overlapsDate(e.startAt, e.endAt, date))
}

/** 월 보기: 그달 1일이 든 주 월요일부터 6주(42일) */
export function monthGrid(anchor: LocalDate): LocalDate[] {
  const first = `${anchor.slice(0, 8)}01`
  const start = weekStartOf(first)
  return Array.from({ length: 42 }, (_, i) => shiftDate(start, i))
}

/** 오늘 이후 n일 안에 시작하거나 진행 중인 일정 (시작 순) */
export function upcoming(events: ScheduleEvent[], now: string, days = 7): ScheduleEvent[] {
  const until = shiftDate(datePart(now), days)
  return sortBy(
    events.filter((e) => e.endAt >= now && datePart(e.startAt) <= until),
    (e) => e.startAt,
  )
}
