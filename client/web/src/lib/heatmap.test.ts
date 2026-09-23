import { describe, expect, it } from 'vitest'
import { buildYearGrid, countByDate, currentStreak, heatLevel, monthLabels, totalIn } from './heatmap'

describe('countByDate', () => {
  it('여러 도메인 날짜를 날짜별로 합산한다', () => {
    const counts = countByDate(['2026-09-21', '2026-09-21'], ['2026-09-21', '2026-09-22'], [])
    expect(counts.get('2026-09-21')).toBe(3)
    expect(counts.get('2026-09-22')).toBe(1)
    expect(counts.get('2026-09-23')).toBeUndefined()
  })
})

describe('heatLevel', () => {
  it('0건은 0, 최댓값은 4, 그 사이는 1~3', () => {
    expect(heatLevel(0, 8)).toBe(0)
    expect(heatLevel(1, 8)).toBe(1)
    expect(heatLevel(2, 8)).toBe(1)
    expect(heatLevel(3, 8)).toBe(2)
    expect(heatLevel(6, 8)).toBe(3)
    expect(heatLevel(8, 8)).toBe(4)
  })
})

describe('buildYearGrid', () => {
  const counts = countByDate(['2026-09-23', '2026-09-23', '2026-09-22'])
  const grid = buildYearGrid(counts, '2026-09-23')

  it('53주 × 7일, 열은 일요일부터', () => {
    expect(grid).toHaveLength(53)
    grid.forEach((col) => expect(col).toHaveLength(7))
    expect(grid[52][0].date).toBe('2026-09-20') // 일요일
  })

  it('마지막 열에 오늘이 있고 이후는 future', () => {
    const last = grid[52]
    expect(last[3].date).toBe('2026-09-23')
    expect(last[3].count).toBe(2)
    expect(last[3].level).toBe(4)
    expect(last[2].level).toBe(2)
    expect(last[4].future).toBe(true)
    expect(last[4].level).toBe(0)
  })

  it('첫 열은 52주 전 일요일', () => {
    expect(grid[0][0].date).toBe('2025-09-21')
  })

  it('월 라벨은 1일이 든 주에 붙는다', () => {
    const labels = monthLabels(grid)
    const octIdx = grid.findIndex((col) => col.some((c) => c.date === '2025-10-01'))
    expect(labels[octIdx]).toBe('10월')
    expect(labels.filter(Boolean).length).toBeGreaterThanOrEqual(12)
  })
})

describe('currentStreak', () => {
  it('오늘부터 거꾸로 연속 일수', () => {
    const counts = countByDate(['2026-09-23', '2026-09-22', '2026-09-21', '2026-09-19'])
    expect(currentStreak(counts, '2026-09-23')).toBe(3)
  })
  it('오늘 기록이 없으면 어제부터', () => {
    const counts = countByDate(['2026-09-22', '2026-09-21'])
    expect(currentStreak(counts, '2026-09-23')).toBe(2)
  })
  it('어제도 없으면 0', () => {
    expect(currentStreak(countByDate(['2026-09-20']), '2026-09-23')).toBe(0)
  })
})

describe('totalIn', () => {
  it('기간 안 건수 합', () => {
    const counts = countByDate(['2026-09-21', '2026-09-23', '2026-09-28'])
    expect(totalIn(counts, '2026-09-21', '2026-09-27')).toBe(2)
  })
})
