import { addDays } from 'date-fns'
import { parseLocalDate, shiftDate, toLocalDate, type LocalDate } from './date'

/** 기록 날짜들을 날짜별 건수로 합산한다 (잔디 전용 API가 없어 프론트에서 모은다) */
export function countByDate(...sources: LocalDate[][]): Map<LocalDate, number> {
  const counts = new Map<LocalDate, number>()
  for (const dates of sources) {
    for (const d of dates) counts.set(d, (counts.get(d) ?? 0) + 1)
  }
  return counts
}

export type HeatLevel = 0 | 1 | 2 | 3 | 4

/** 최댓값 대비 4단계로 나눈다. 0건은 0단계 */
export function heatLevel(count: number, max: number): HeatLevel {
  if (count <= 0 || max <= 0) return 0
  return Math.min(4, Math.max(1, Math.ceil((count / max) * 4))) as HeatLevel
}

export interface HeatCell {
  date: LocalDate
  count: number
  level: HeatLevel
  future: boolean
}

/**
 * GitHub 잔디처럼 일요일 시작 7행 × 53주 격자. end가 들어 있는 주가 마지막 열이다.
 * 반환값은 열(주) 우선 순서이며, end 이후 날짜는 future로 표시한다.
 */
export function buildYearGrid(counts: Map<LocalDate, number>, end: LocalDate, weeks = 53): HeatCell[][] {
  const endDate = parseLocalDate(end)
  const lastSunday = addDays(endDate, -endDate.getDay())
  const first = addDays(lastSunday, -(weeks - 1) * 7)
  let max = 0
  const cols: { date: LocalDate; count: number; future: boolean }[][] = []
  for (let w = 0; w < weeks; w++) {
    const col = []
    for (let d = 0; d < 7; d++) {
      const date = toLocalDate(addDays(first, w * 7 + d))
      const future = date > end
      const count = future ? 0 : (counts.get(date) ?? 0)
      if (count > max) max = count
      col.push({ date, count, future })
    }
    cols.push(col)
  }
  return cols.map((col) => col.map((c) => ({ ...c, level: heatLevel(c.count, max) })))
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

/** 격자 열마다 월 라벨 — 그 주에 1일이 들어 있으면 "Sep"처럼, 아니면 빈 문자열 (이슈 #201에서 영어로) */
export function monthLabels(grid: HeatCell[][]): string[] {
  const labels = grid.map((col) => {
    const firstOfMonth = col.find((c) => c.date.endsWith('-01'))
    return firstOfMonth ? MONTHS[Number(firstOfMonth.date.slice(5, 7)) - 1] : ''
  })
  // 첫 열은 월 이름이 없으면 그 달을 붙이되, 다음 달 라벨과 겹치면(3열 안) 생략
  if (grid.length && !labels[0] && !labels.slice(1, 3).some(Boolean)) {
    labels[0] = MONTHS[Number(grid[0][0].date.slice(5, 7)) - 1]
  }
  return labels
}

/** 오늘까지 연속 기록 일수. 오늘 기록이 아직 없으면 어제부터 센다 */
export function currentStreak(counts: Map<LocalDate, number>, today: LocalDate): number {
  let day = (counts.get(today) ?? 0) > 0 ? today : shiftDate(today, -1)
  let streak = 0
  while ((counts.get(day) ?? 0) > 0) {
    streak++
    day = shiftDate(day, -1)
  }
  return streak
}

export function totalIn(counts: Map<LocalDate, number>, from: LocalDate, to: LocalDate): number {
  let sum = 0
  for (const [d, c] of counts) if (d >= from && d <= to) sum += c
  return sum
}
