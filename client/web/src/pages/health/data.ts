import { useQuery } from '@tanstack/react-query'
import { healthWeeklyStats, mealWeeklyStats } from '../../api/analytics'
import { healthLogs, workoutLogs } from '../../api/health'
import { BIG_PAGE, useList } from '../../api/resource'
import type { MealType } from '../../api/types'
import { toLocalDateTime, type LocalDate } from '../../lib/date'

export const SLEEP_GOAL = 7

const DEFAULT_TIME: Record<MealType, string> = { BREAKFAST: '08:00', LUNCH: '12:30', DINNER: '18:30', SNACK: '15:30' }

/** 끼니 기록 시각: 오늘이면 지금, 아니면 끼니별 기본 시각 */
export function defaultMealTime(type: MealType, date: LocalDate, today: LocalDate): string {
  if (date === today) return toLocalDateTime(new Date()).slice(11, 16)
  return DEFAULT_TIME[type]
}

export function useWorkouts() {
  return useList(workoutLogs, { size: BIG_PAGE, sort: 'performedAt,desc' })
}
export function useBodyLogs() {
  return useList(healthLogs, { size: BIG_PAGE, sort: 'recordedAt,desc' })
}
export function useMealStats() {
  return useQuery({
    queryKey: [mealWeeklyStats.path, 'list', { size: 8 }],
    queryFn: () => mealWeeklyStats.list({ size: 8, sort: 'weekStart,desc' }),
  })
}
export function useBodyStats() {
  return useQuery({
    queryKey: [healthWeeklyStats.path, 'list', { size: 8 }],
    queryFn: () => healthWeeklyStats.list({ size: 8, sort: 'weekStart,desc' }),
  })
}
