import { describe, expect, it } from 'vitest'
import type { MealItem, MealRecord } from '../../api/types'
import { dailySeries, goalState, kcalOf, macroShare, mealsOn } from './diet'

const item = (name: string, c: number, f: number, p: number): MealItem => ({
  id: 0,
  mealRecordId: 0,
  name,
  calories: 0,
  carbsG: c,
  fatG: f,
  proteinG: p,
  sodiumMg: null,
  createdAt: '',
  updatedAt: '',
})
const rec = (id: number, consumedAt: string, mealType: MealRecord['mealType'], items: MealItem[]): MealRecord => ({
  id,
  consumedAt,
  mealType,
  notes: null,
  items,
  totals: { calories: 0, carbsG: 0, proteinG: 0, fatG: 0, sodiumMg: 0 },
  createdAt: '',
  updatedAt: '',
})

describe('식단 계산', () => {
  it('칼로리 = 탄수×4 + 지방×9 + 단백질×4', () => {
    expect(kcalOf({ carbsG: 50, fatG: 10, proteinG: 30 })).toBe(410)
    expect(kcalOf({ carbsG: 0.5, fatG: 0, proteinG: 0 })).toBe(2)
  })
  it('그날 끼니별로 합치고 제목을 이어 붙인다', () => {
    const records = [
      rec(1, '2026-09-24T08:00:00', 'BREAKFAST', [item('그릭요거트', 12, 5, 17), item('바나나', 27, 0, 1)]),
      rec(2, '2026-09-24T12:30:00', 'LUNCH', [item('닭가슴살 덮밥', 58, 8, 42)]),
      rec(3, '2026-09-23T19:00:00', 'DINNER', [item('어제 저녁', 50, 10, 30)]),
    ]
    const m = mealsOn(records, '2026-09-24')
    expect(m.BREAKFAST?.title).toBe('그릭요거트, 바나나')
    expect(m.BREAKFAST?.carbsG).toBe(39)
    expect(m.BREAKFAST?.kcal).toBe(kcalOf({ carbsG: 39, fatG: 5, proteinG: 18 }))
    expect(m.LUNCH?.records).toHaveLength(1)
    expect(m.DINNER).toBeUndefined()
  })
  it('최근 n일 합계는 기록 없는 날도 0으로 채운다', () => {
    const s = dailySeries([rec(1, '2026-09-24T08:00:00', 'BREAKFAST', [item('a', 10, 1, 5)])], '2026-09-24', 3)
    expect(s.map((d) => d.date)).toEqual(['2026-09-22', '2026-09-23', '2026-09-24'])
    expect(s[0]).toMatchObject({ kcal: 0, meals: 0 })
    expect(s[2]).toMatchObject({ carbsG: 10, fatG: 1, proteinG: 5, kcal: 69, meals: 1 })
  })
  it('목표 상태', () => {
    expect(goalState(100, null, 'AT_MOST')).toBe('none')
    expect(goalState(141, 140, 'AT_MOST')).toBe('over')
    expect(goalState(140, 140, 'AT_MOST')).toBe('ok')
    expect(goalState(150, 160, 'AT_LEAST')).toBe('under')
    expect(goalState(160, 160, 'AT_LEAST')).toBe('ok')
  })
  it('칼로리 비율', () => {
    expect(macroShare({ carbsG: 50, fatG: 10, proteinG: 30 })).toEqual({ carbsG: 49, fatG: 22, proteinG: 29 })
    expect(macroShare({ carbsG: 0, fatG: 0, proteinG: 0 })).toEqual({ carbsG: 0, fatG: 0, proteinG: 0 })
  })
})
