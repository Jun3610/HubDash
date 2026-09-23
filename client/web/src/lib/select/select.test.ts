import { describe, expect, it } from 'vitest'
import type { Assignment, Reminder, ScheduleEvent } from '../../api/types'
import { homeAssignments, weekCompletion } from './pknu'
import { pendingReminders, urgency, whenLabel } from './reminder'
import { eventsOn, eventTimeLabel } from './schedule'

const base = { createdAt: '', updatedAt: '' }
const asg = (id: number, dueDate: string, completed: boolean): Assignment => ({
  ...base,
  id,
  courseId: 1,
  title: `a${id}`,
  dueDate,
  completed,
  notes: null,
})
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

describe('과제', () => {
  const today = '2026-09-23'
  const list = [
    asg(1, '2026-09-26', false),
    asg(2, '2026-09-24', false),
    asg(3, '2026-09-20', false), // 지난 미완료
    asg(4, '2026-09-22', true),
    asg(5, '2026-08-01', true), // 오래된 완료
  ]
  it('미완료 마감 순 → 최근 완료로 채움', () => {
    expect(homeAssignments(list, today).map((a) => a.id)).toEqual([3, 2, 1, 4])
  })
  it('이번 주(09-21~27) 완료율', () => {
    expect(weekCompletion(list, today)).toEqual({ done: 1, total: 3, rate: 1 / 3 })
  })
  it('이번 주 과제가 없으면 0', () => {
    expect(weekCompletion([], today).rate).toBe(0)
  })
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

describe('리마인더', () => {
  const r = (id: number, targetAt: string, sent: boolean): Reminder => ({
    ...base,
    id,
    title: '',
    targetAt,
    sent,
    targetDomain: null,
    targetEntityId: null,
  })
  it('안 보낸 것만 시각 순', () => {
    const list = [
      r(1, '2026-09-25T09:00:00', false),
      r(2, '2026-09-24T09:00:00', false),
      r(3, '2026-09-20T09:00:00', true),
    ]
    expect(pendingReminders(list).map((x) => x.id)).toEqual([2, 1])
  })
  it('표기', () => {
    expect(whenLabel('2026-09-24T09:00:00', '2026-09-23')).toBe('내일 09:00')
    expect(whenLabel('2026-09-23T21:00:00', '2026-09-23')).toBe('오늘 21:00')
    expect(whenLabel('2026-09-26T10:00:00', '2026-09-23')).toBe('09.26')
  })
  it('급한 정도', () => {
    const now = '2026-09-23T12:00:00'
    expect(urgency('2026-09-23T11:00:00', now)).toBe('overdue')
    expect(urgency('2026-09-24T09:00:00', now)).toBe('soon')
    expect(urgency('2026-09-26T09:00:00', now)).toBe('near')
    expect(urgency('2026-09-30T09:00:00', now)).toBe('later')
  })
})
