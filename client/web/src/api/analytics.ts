import { useMutation, useQueryClient } from '@tanstack/react-query'
import { http, type Query } from './client'
import { domainOf, invalidateDomain } from './resource'
import type {
  AssignmentWeeklyStat,
  BatchRunResult,
  HabitWeeklyStat,
  HealthLogWeeklyStat,
  MealWeeklyStat,
  Page,
  StudyTopicWeeklyStat,
} from './types'
import type { LocalDate } from '../lib/date'

export interface WeeklyStat<T> {
  key: string
  label: string
  path: string
  /** 목록 필수 쿼리 이름 (없으면 null) */
  requiredParam: string | null
  list(query?: Query): Promise<Page<T>>
  run(weekStart: LocalDate): Promise<BatchRunResult>
}

function defineStat<T>(key: string, label: string, path: string, requiredParam: string | null): WeeklyStat<T> {
  return {
    key,
    label,
    path,
    requiredParam,
    list: (query) => http.get<Page<T>>(path, query),
    run: (weekStart) => http.post<BatchRunResult>(`${path}/batch-runs`, { weekStart }),
  }
}

export const mealWeeklyStats = defineStat<MealWeeklyStat>(
  'meal',
  '식단',
  '/api/health/analytics/meal-weekly-stats',
  null,
)
export const healthWeeklyStats = defineStat<HealthLogWeeklyStat>(
  'health',
  '체중·수면',
  '/api/health/analytics/weekly-stats',
  null,
)
export const studyWeeklyStats = defineStat<StudyTopicWeeklyStat>(
  'study',
  '공부',
  '/api/study/analytics/topic-weekly-stats',
  'topicId',
)
export const habitWeeklyStats = defineStat<HabitWeeklyStat>(
  'habit',
  '습관',
  '/api/life/analytics/habit-weekly-stats',
  'habitId',
)
export const assignmentWeeklyStats = defineStat<AssignmentWeeklyStat>(
  'assignment',
  '과제',
  '/api/pknu/analytics/assignment-weekly-stats',
  'courseId',
)

export const WEEKLY_STATS = [
  mealWeeklyStats,
  healthWeeklyStats,
  studyWeeklyStats,
  habitWeeklyStats,
  assignmentWeeklyStats,
] as const

export function useRunWeeklyStat(stat: WeeklyStat<unknown>) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (weekStart: LocalDate) => stat.run(weekStart),
    onSuccess: () => invalidateDomain(qc, domainOf(stat.path)),
  })
}
