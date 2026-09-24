import {
  MEAL_TYPE_KO,
  MEAL_TYPES,
  type Habit,
  type HabitLog,
  type HealthLog,
  type MealRecord,
  type ScheduleEvent,
  type StudyProgress,
  type StudyTopic,
  type WorkoutLog,
} from '../../api/types'
import type { ActivityDomain } from '../../hooks/useActivity'
import { datePart, formatMinutes, formatTime, type LocalDate } from '../date'
import { mealsOn } from './diet'

export interface ActivitySources {
  meals: MealRecord[]
  workouts: WorkoutLog[]
  body: HealthLog[]
  progresses: StudyProgress[]
  topics: StudyTopic[]
  habitLogs: HabitLog[]
  habits: Habit[]
  events: ScheduleEvent[]
}

export interface ActivityItem {
  domain: ActivityDomain
  text: string
  to: string
}

/** 사이드바에서 날짜를 누르면 보여 줄 그날 기록 목록 (이슈 #132) */
export function activityOn(src: ActivitySources, date: LocalDate): ActivityItem[] {
  const out: ActivityItem[] = []
  const meals = mealsOn(src.meals, date)
  for (const t of MEAL_TYPES) {
    const m = meals[t]
    if (m)
      out.push({
        domain: 'meal',
        text: `${MEAL_TYPE_KO[t]} ${m.title || '기록'} · ${m.kcal}kcal`,
        to: '/health?tab=meal',
      })
  }
  for (const w of src.workouts) {
    if (w.performedAt === date)
      out.push({ domain: 'workout', text: `${w.type} ${formatMinutes(w.durationMinutes)}`, to: '/health?tab=workout' })
  }
  for (const b of src.body) {
    if (datePart(b.recordedAt) !== date) continue
    const parts = [b.weightKg != null && `${b.weightKg}kg`, b.sleepHours != null && `수면 ${b.sleepHours}시간`].filter(
      Boolean,
    )
    out.push({
      domain: 'body',
      text: `${formatTime(b.recordedAt)} ${parts.join(' · ') || '기록'}`,
      to: '/health?tab=body',
    })
  }
  const topicName = new Map(src.topics.map((t) => [t.id, t.name]))
  for (const p of src.progresses) {
    if (p.studiedAt === date)
      out.push({
        domain: 'study',
        text: `${topicName.get(p.topicId) ?? '공부'} ${formatMinutes(p.minutes)}`,
        to: `/study`,
      })
  }
  const habitName = new Map(src.habits.map((h) => [h.id, h.name]))
  for (const l of src.habitLogs) {
    if (l.completed && l.performedAt === date)
      out.push({ domain: 'habit', text: `${habitName.get(l.habitId) ?? '습관'} 완료`, to: '/memo?tab=habits' })
  }
  for (const e of src.events) {
    if (datePart(e.startAt) === date)
      out.push({ domain: 'event', text: e.allDay ? e.title : `${formatTime(e.startAt)} ${e.title}`, to: '/schedule' })
  }
  return out
}
