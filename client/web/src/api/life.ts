import { defineResource } from './resource'
import type { Habit, HabitLog, HabitLogRequest, HabitRequest, ReadingLog, ReadingLogRequest } from './types'

export const habits = defineResource<Habit, HabitRequest>('/api/life/habits')
/** 목록 필수 쿼리: habitId */
export const habitLogs = defineResource<HabitLog, HabitLogRequest>('/api/life/habit-logs')
export const readingLogs = defineResource<ReadingLog, ReadingLogRequest>('/api/life/reading-logs')
