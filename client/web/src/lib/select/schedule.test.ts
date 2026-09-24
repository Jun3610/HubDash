import { describe, expect, it } from 'vitest'
import type { ScheduleEvent } from '../../api/types'
import { allDayOn, eventsOn, monthGrid, placeDay, searchEvents, timeRange, upcoming } from './schedule'

const ev = (id: number, startAt: string, endAt: string | null, allDay = false): ScheduleEvent => ({
  id,
  title: `e${id}`,
  startAt,
  endAt,
  allDay,
  location: null,
  description: null,
  createdAt: '',
  updatedAt: '',
})
const D = '2026-09-23'

describe('주 보기 배치', () => {
  it('08:00 기준 분 단위 위치', () => {
    const [p] = placeDay([ev(1, `${D}T10:30:00`, `${D}T12:00:00`)], D)
    expect(p.top).toBe(150)
    expect(p.height).toBe(90)
    expect(p.lanes).toBe(1)
  })
  it('겹치면 줄을 나누고, 안 겹치면 한 줄', () => {
    const placed = placeDay(
      [
        ev(1, `${D}T10:00:00`, `${D}T12:00:00`),
        ev(2, `${D}T11:00:00`, `${D}T13:00:00`),
        ev(3, `${D}T15:00:00`, `${D}T16:00:00`),
      ],
      D,
    )
    const by = new Map(placed.map((p) => [p.event.id, p]))
    expect(by.get(1)!.lanes).toBe(2)
    expect(by.get(2)!.lane).toBe(1)
    expect(by.get(3)!.lanes).toBe(1)
    expect(by.get(3)!.lane).toBe(0)
  })
  it('끝난 줄은 다시 쓴다', () => {
    const placed = placeDay(
      [
        ev(1, `${D}T10:00:00`, `${D}T11:00:00`),
        ev(2, `${D}T10:30:00`, `${D}T12:00:00`),
        ev(3, `${D}T11:00:00`, `${D}T11:30:00`),
      ],
      D,
    )
    const by = new Map(placed.map((p) => [p.event.id, p]))
    expect(by.get(3)!.lane).toBe(0)
    expect(by.get(3)!.lanes).toBe(2)
  })
  it('격자 밖은 잘라 붙이고 표시', () => {
    const [early] = placeDay([ev(1, `${D}T06:00:00`, `${D}T09:00:00`)], D)
    expect(early.top).toBe(0)
    expect(early.height).toBe(60)
    expect(early.clippedTop).toBe(true)
    const [late] = placeDay([ev(2, `${D}T21:30:00`, `${D}T23:30:00`)], D)
    expect(late.top + late.height).toBe(14 * 60)
    expect(late.clippedBottom).toBe(true)
  })
  it('자정을 넘는 일정은 날짜별로 나눠진다', () => {
    const e = ev(1, '2026-09-22T21:00:00', `${D}T09:00:00`)
    const [p] = placeDay([e], D)
    expect(p.top).toBe(0)
    expect(p.height).toBe(60)
    expect(p.clippedTop).toBe(true)
  })
  it('종일 일정은 시간 격자에서 빠지고 종일 줄에', () => {
    const e = ev(1, `${D}T00:00:00`, '2026-09-25T23:59:00', true)
    expect(placeDay([e], D)).toHaveLength(0)
    expect(allDayOn([e], '2026-09-24')).toHaveLength(1)
  })
})

describe('월 보기 · 다가오는 일정', () => {
  it('6주 격자, 월요일 시작', () => {
    const g = monthGrid('2026-09-23')
    expect(g).toHaveLength(42)
    expect(g[0]).toBe('2026-08-31')
    expect(g).toContain('2026-09-30')
  })
  it('다가오는 7일 (진행 중 포함, 끝난 것 제외)', () => {
    const list = [
      ev(1, `${D}T09:00:00`, `${D}T10:00:00`), // 끝남
      ev(2, `${D}T13:00:00`, `${D}T15:00:00`), // 진행 중
      ev(3, '2026-09-30T10:00:00', '2026-09-30T11:00:00'), // 7일째
      ev(4, '2026-10-01T10:00:00', '2026-10-01T11:00:00'), // 8일째
    ]
    expect(upcoming(list, `${D}T14:00:00`).map((e) => e.id)).toEqual([2, 3])
  })
})

describe('끝 시각 없는 일정 (이슈 #156)', () => {
  const e = ev(9, `${D}T19:00:00`, null)
  it('시작한 날에만 보이고 시각은 시작만', () => {
    expect(eventsOn([e], D)).toHaveLength(1)
    expect(eventsOn([e], '2026-09-24')).toHaveLength(0)
    expect(timeRange(e)).toBe('19:00')
    expect(timeRange(ev(1, `${D}T10:00:00`, `${D}T11:30:00`))).toBe('10:00–11:30')
  })
  it('주 보기에는 30분 칸, 다가오는 일정은 시작 기준', () => {
    const [p] = placeDay([e], D)
    expect(p.height).toBe(30)
    expect(upcoming([e], `${D}T18:00:00`)).toHaveLength(1)
    expect(upcoming([e], `${D}T20:00:00`)).toHaveLength(0)
  })
})

describe('일정 검색 (이슈 #160)', () => {
  const list = [
    { ...ev(1, '2026-09-01T10:00:00', null), title: '프젝회의' },
    { ...ev(2, '2026-09-20T10:00:00', null), title: '운동', location: '헬스장' },
    { ...ev(3, '2026-09-10T10:00:00', null), title: 'OP6 출근', description: '프로젝트 발표' },
  ]
  it('제목·장소·메모에서 찾고 최근 순', () => {
    expect(searchEvents(list, '프').map((e) => e.id)).toEqual([3, 1])
    expect(searchEvents(list, '헬스').map((e) => e.id)).toEqual([2])
    expect(searchEvents(list, 'op6').map((e) => e.id)).toEqual([3])
    expect(searchEvents(list, '  ')).toEqual([])
  })
})
