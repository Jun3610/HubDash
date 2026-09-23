import { describe, expect, it } from 'vitest'
import type { StudyProgress } from '../../api/types'
import { longestStreak, studyStreak, weeklyMinutes } from './study'

const p = (studiedAt: string, minutes: number): StudyProgress => ({
  id: 0,
  topicId: 1,
  studiedAt,
  minutes,
  notes: null,
  createdAt: '',
  updatedAt: '',
})

describe('공부', () => {
  it('최근 8주 주별 합계 (이번 주가 마지막)', () => {
    const w = weeklyMinutes(
      [p('2026-09-23', 50), p('2026-09-21', 30), p('2026-09-20', 40), p('2026-07-01', 99)],
      '2026-09-23',
    )
    expect(w).toHaveLength(8)
    expect(w[7]).toEqual({ weekStart: '2026-09-21', minutes: 80 })
    expect(w[6]).toEqual({ weekStart: '2026-09-14', minutes: 40 })
    expect(w[0].weekStart).toBe('2026-08-03')
    expect(w.reduce((a, x) => a + x.minutes, 0)).toBe(120) // 8주 밖은 제외
  })
  it('최장 연속', () => {
    expect(longestStreak(['2026-09-01', '2026-09-02', '2026-09-03', '2026-09-05', '2026-09-02'])).toBe(3)
    expect(longestStreak([])).toBe(0)
  })
  it('현재 연속', () => {
    expect(studyStreak([p('2026-09-22', 10), p('2026-09-21', 10)], '2026-09-23')).toBe(2)
  })
})
