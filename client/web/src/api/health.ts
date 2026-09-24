import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { http } from './client'
import { defineResource } from './resource'
import type {
  DailyMealSummary,
  DietGoal,
  DietGoalRequest,
  HealthLog,
  HealthLogRequest,
  MealItem,
  MealItemRequest,
  MealRecord,
  MealRecordRequest,
  WorkoutLog,
  WorkoutLogRequest,
} from './types'
import type { LocalDate } from '../lib/date'

export const mealRecords = defineResource<MealRecord, MealRecordRequest>('/api/health/meal-records')
/** 목록 필수 쿼리: mealRecordId */
export const mealItems = defineResource<MealItem, MealItemRequest>('/api/health/meal-items')
export const workoutLogs = defineResource<WorkoutLog, WorkoutLogRequest>('/api/health/workout-logs')
export const healthLogs = defineResource<HealthLog, HealthLogRequest>('/api/health/logs')

const DAILY_SUMMARY = '/api/health/meal-records/daily-summary'

export function fetchDailySummary(date: LocalDate) {
  return http.get<DailyMealSummary>(DAILY_SUMMARY, { date })
}

export function useDailySummary(date: LocalDate) {
  return useQuery({ queryKey: [DAILY_SUMMARY, date], queryFn: () => fetchDailySummary(date) })
}

const DIET_GOAL = '/api/health/diet-goal'

export function useDietGoal() {
  return useQuery({ queryKey: [DIET_GOAL], queryFn: () => http.get<DietGoal>(DIET_GOAL) })
}

export function useUpdateDietGoal() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: DietGoalRequest) => http.put<DietGoal>(DIET_GOAL, body),
    onSuccess: (data) => qc.setQueryData([DIET_GOAL], data),
  })
}
