import { describe, expect, it } from 'vitest'
import type { HabitLog, ReadingLog } from '../../api/types'
import { cellState, lastDays, recentRate, sortBooks, stars } from './life'

const log = (performedAt: string, completed: boolean): HabitLog => ({
  id: 0,
  habitId: 1,
  performedAt,
  completed,
  notes: null,
  createdAt: '',
  updatedAt: '',
})
const book = (id: number, startedAt: string, finishedAt: string | null): ReadingLog => ({
  id,
  title: '',
  author: null,
  startedAt,
  finishedAt,
  rating: null,
  notes: null,
  createdAt: '',
  updatedAt: '',
})

describe('습관 트래커', () => {
  const today = '2026-09-23'
  const logs = [log('2026-09-23', true), log('2026-09-22', false), log('2026-09-20', true)]
  it('칸 상태', () => {
    expect(cellState(logs, '2026-09-23', today)).toBe('done')
    expect(cellState(logs, '2026-09-22', today)).toBe('missed')
    expect(cellState(logs, '2026-09-21', today)).toBe('missed') // 지난 날 기록 없음
    expect(cellState([], '2026-09-23', today)).toBe('empty') // 오늘 아직
  })
  it('최근 14일', () => {
    const d = lastDays(today, 14)
    expect(d).toHaveLength(14)
    expect(d[0]).toBe('2026-09-10')
    expect(d[13]).toBe(today)
  })
  it('최근 7일 달성 — 오늘 기록이 없으면 분모 6', () => {
    expect(recentRate(logs, today)).toEqual({ done: 2, total: 7 })
    expect(recentRate([log('2026-09-20', true)], today)).toEqual({ done: 1, total: 6 })
  })
})

describe('독서', () => {
  it('읽는 중 먼저, 완독은 최근 순', () => {
    const list = [book(1, '2026-01-01', '2026-02-01'), book(2, '2026-09-01', null), book(3, '2026-03-01', '2026-08-01')]
    expect(sortBooks(list).map((b) => b.id)).toEqual([2, 3, 1])
  })
  it('별점', () => {
    expect(stars(4)).toBe('★★★★☆')
    expect(stars(null)).toBe('—')
  })
})
