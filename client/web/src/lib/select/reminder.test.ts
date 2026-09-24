import { describe, expect, it } from 'vitest'
import type { Reminder } from '../../api/types'
import { groupReminders, presetDayBefore, presetOneHour, presetTonight, snooze, untilLabel } from './reminder'

const r = (id: number, targetAt: string, sent = false): Reminder => ({
  id,
  title: '',
  targetAt,
  sent,
  targetDomain: null,
  targetEntityId: null,
  createdAt: '',
  updatedAt: '',
})
const NOW = '2026-09-24T14:00:00'

describe('리마인더 묶음', () => {
  it('지남 / 오늘 / 예정 / 보냄', () => {
    const g = groupReminders(
      [
        r(1, '2026-09-23T21:00:00'),
        r(2, '2026-09-24T13:59:00'),
        r(3, '2026-09-24T18:30:00'),
        r(4, '2026-09-25T09:00:00'),
        r(5, '2026-09-20T09:00:00', true),
        r(6, '2026-09-22T09:00:00', true),
      ],
      NOW,
    )
    expect(g.overdue.map((x) => x.id)).toEqual([1, 2])
    expect(g.today.map((x) => x.id)).toEqual([3])
    expect(g.upcoming.map((x) => x.id)).toEqual([4])
    expect(g.sent.map((x) => x.id)).toEqual([6, 5])
  })
  it('남은 시간', () => {
    expect(untilLabel('2026-09-24T14:30:00', NOW)).toBe('30분 뒤')
    expect(untilLabel('2026-09-24T18:30:00', NOW)).toBe('4시간 뒤')
    expect(untilLabel('2026-09-23T21:00:00', NOW)).toBe('17시간 지남')
    expect(untilLabel('2026-09-27T14:00:00', NOW)).toBe('3일 뒤')
  })
})

describe('빠른 선택 · 미루기', () => {
  it('1시간 뒤 (자정 넘김 포함)', () => {
    expect(presetOneHour('2026-09-24T14:07:33')).toBe('2026-09-24T15:07:00')
    expect(presetOneHour('2026-09-24T23:30:00')).toBe('2026-09-25T00:30:00')
  })
  it('오늘 밤 9시, 지났으면 내일', () => {
    expect(presetTonight(NOW)).toBe('2026-09-24T21:00:00')
    expect(presetTonight('2026-09-24T21:30:00')).toBe('2026-09-25T21:00:00')
  })
  it('마감 하루 전 21시', () => {
    expect(presetDayBefore('2026-10-01')).toBe('2026-09-30T21:00:00')
  })
  it('미루기: 앞으로 올 알림은 그 시각 기준, 지난 알림은 지금 기준', () => {
    expect(snooze('2026-09-24T18:30:00', NOW, '1h')).toBe('2026-09-24T19:30:00')
    expect(snooze('2026-09-24T18:30:00', NOW, 'tomorrow')).toBe('2026-09-25T18:30:00')
    expect(snooze('2026-09-23T21:00:00', NOW, '1h')).toBe('2026-09-24T15:00:00')
  })
})
