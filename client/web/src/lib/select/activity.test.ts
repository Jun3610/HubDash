import { describe, expect, it } from 'vitest'
import type { ActivitySources } from './activity'
import { activityOn } from './activity'

const ts = { createdAt: '', updatedAt: '' }
const src: ActivitySources = {
  meals: [
    {
      ...ts,
      id: 1,
      consumedAt: '2026-09-22T08:00:00',
      mealType: 'BREAKFAST',
      notes: null,
      totals: { calories: 0, carbsG: 0, proteinG: 0, fatG: 0, sodiumMg: 0 } as never,
      items: [
        {
          ...ts,
          id: 1,
          mealRecordId: 1,
          name: '오트밀',
          calories: 0,
          carbsG: 50,
          fatG: 5,
          proteinG: 10,
          sodiumMg: null,
        },
      ],
    },
  ],
  workouts: [
    { ...ts, id: 1, performedAt: '2026-09-22', type: '러닝', durationMinutes: 40, caloriesBurned: null, notes: null },
  ],
  body: [
    { ...ts, id: 1, recordedAt: '2026-09-22T07:30:00', weightKg: 70.2, sleepHours: null, notes: null },
    { ...ts, id: 2, recordedAt: '2026-09-23T07:30:00', weightKg: 70, sleepHours: null, notes: null },
  ],
  progresses: [{ ...ts, id: 1, topicId: 9, studiedAt: '2026-09-22', minutes: 90, notes: null }],
  topics: [{ ...ts, id: 9, name: '자료구조', description: null, notionUrl: null }],
  habitLogs: [
    { ...ts, id: 1, habitId: 3, performedAt: '2026-09-22', completed: true, notes: null },
    { ...ts, id: 2, habitId: 3, performedAt: '2026-09-22', completed: false, notes: null },
  ],
  habits: [{ ...ts, id: 3, name: '물 2L', description: null }],
  events: [
    {
      ...ts,
      id: 1,
      title: '팀 회의',
      startAt: '2026-09-22T19:00:00',
      endAt: '2026-09-22T20:00:00',
      location: null,
      description: null,
      allDay: false,
    },
    {
      ...ts,
      id: 2,
      title: '추석',
      startAt: '2026-09-22T00:00:00',
      endAt: '2026-09-23T00:00:00',
      location: null,
      description: null,
      allDay: true,
    },
  ],
}

describe('activityOn', () => {
  it('그날 기록만 도메인별로 모은다', () => {
    const items = activityOn(src, '2026-09-22')
    expect(items.map((i) => i.text)).toEqual([
      '아침 오트밀 · 285kcal',
      '러닝 40m',
      '07:30 70.2kg',
      '자료구조 1h 30m',
      '물 2L 완료',
      '19:00 팀 회의',
      '추석',
    ])
  })

  it('기록 없는 날은 빈 목록', () => {
    expect(activityOn(src, '2026-09-21')).toEqual([])
  })
})
