import { describe, expect, it } from 'vitest'
import { overlapsDate, sortBy, withinDates, withinDateTimes } from './range'

describe('기간 필터', () => {
  const logs = [{ d: '2026-09-20' }, { d: '2026-09-21' }, { d: '2026-09-27' }, { d: '2026-09-28' }]

  it('LocalDate 양 끝 포함', () => {
    expect(withinDates(logs, (l) => l.d, '2026-09-21', '2026-09-27').map((l) => l.d)).toEqual([
      '2026-09-21',
      '2026-09-27',
    ])
  })

  it('LocalDateTime은 날짜 부분으로 비교 (자정 직전 포함)', () => {
    const meals = [{ at: '2026-09-23T23:59:59.999' }, { at: '2026-09-24T00:00:00' }]
    expect(withinDateTimes(meals, (m) => m.at, '2026-09-23', '2026-09-23')).toHaveLength(1)
  })

  it('여러 날 걸친 일정', () => {
    expect(overlapsDate('2026-09-22T10:00:00', '2026-09-24T12:00:00', '2026-09-23')).toBe(true)
    expect(overlapsDate('2026-09-22T10:00:00', '2026-09-22T12:00:00', '2026-09-23')).toBe(false)
  })

  it('정렬은 원본을 바꾸지 않는다', () => {
    const src = [{ v: 2 }, { v: 1 }]
    expect(sortBy(src, (x) => x.v).map((x) => x.v)).toEqual([1, 2])
    expect(sortBy(src, (x) => x.v, 'desc').map((x) => x.v)).toEqual([2, 1])
    expect(src[0].v).toBe(2)
  })
})
