import { describe, expect, it } from 'vitest'
import type { ScheduleEvent } from '../../api/types'
import { allDayOn, monthGrid, placeDay, upcoming } from './schedule'

const ev = (id: number, startAt: string, endAt: string, allDay = false): ScheduleEvent => ({
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
