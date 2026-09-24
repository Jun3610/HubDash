import { describe, expect, it } from 'vitest'
import type { ScheduleEvent } from '../../api/types'
import { mergeWeekly, semesterStatus, weekNumber } from './pknu'
import { eventsOn, eventTimeLabel } from './schedule'

const base = { createdAt: '', updatedAt: '' }
const ev = (id: number, startAt: string, endAt: string, allDay = false): ScheduleEvent => ({
  ...base,
  id,
  title: `e${id}`,
  startAt,
  endAt,
  allDay,
  location: null,
  description: null,
})

describe('일정', () => {
  const list = [
    ev(1, '2026-09-23T15:00:00', '2026-09-23T16:00:00'),
    ev(2, '2026-09-23T00:00:00', '2026-09-23T23:59:00', true),
    ev(3, '2026-09-22T22:00:00', '2026-09-23T01:00:00'),
    ev(4, '2026-09-24T09:00:00', '2026-09-24T10:00:00'),
  ]
  it('그날 걸친 일정만, 종일 먼저', () => {
    expect(eventsOn(list, '2026-09-23').map((e) => e.id)).toEqual([2, 3, 1])
  })
  it('시각 표기', () => {
    expect(eventTimeLabel(list[0], '2026-09-23')).toBe('15:00')
    expect(eventTimeLabel(list[1], '2026-09-23')).toBe('종일')
    expect(eventTimeLabel(list[2], '2026-09-23')).toBe('이어짐')
  })
})

describe('학기·과목', () => {
  it('학기 상태', () => {
    expect(semesterStatus('2026-09-01', '2026-12-19', '2026-09-23')).toBe('진행 중')
    expect(semesterStatus('2026-09-01', '2026-12-19', '2026-08-31')).toBe('예정')
    expect(semesterStatus('2026-09-01', '2026-12-19', '2026-12-20')).toBe('종료')
  })
  it('주차: 개강 주가 1주차 (2026-09-01 화 개강 → 09-23 수는 4주차)', () => {
    expect(weekNumber('2026-09-01', '2026-12-19', '2026-09-01')).toBe(1)
    expect(weekNumber('2026-09-01', '2026-12-19', '2026-09-06')).toBe(1)
    expect(weekNumber('2026-09-01', '2026-12-19', '2026-09-07')).toBe(2)
    expect(weekNumber('2026-09-01', '2026-12-19', '2026-09-23')).toBe(4)
    expect(weekNumber('2026-09-01', '2026-12-19', '2027-01-01')).toBeNull()
  })
  it('주간 통계 합치기', () => {
    const rows = [
      { weekStart: '2026-09-14', totalCount: 2, completedCount: 1 },
      { weekStart: '2026-09-14', totalCount: 3, completedCount: 3 },
      { weekStart: '2026-09-07', totalCount: 1, completedCount: 1 },
    ]
    expect(mergeWeekly(rows)).toEqual([
      { weekStart: '2026-09-07', total: 1, done: 1, rate: 1 },
      { weekStart: '2026-09-14', total: 5, done: 4, rate: 0.8 },
    ])
  })
})
