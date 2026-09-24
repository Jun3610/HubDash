// 서버 DTO와 1:1로 맞춘 타입. 원본: server/common/src/main/java/.../dto/*Response.java
import type { LocalDate, LocalDateTime } from '../lib/date'

export type Id = number

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface PageQuery {
  page?: number
  size?: number
  sort?: string // "필드,desc"
}

interface Timestamps {
  id: Id
  createdAt: LocalDateTime
  updatedAt: LocalDateTime
}

// ---- PKNU ----
export interface SemesterRequest {
  name: string
  startDate: LocalDate
  endDate: LocalDate
}
export type Semester = SemesterRequest & Timestamps

export interface CourseRequest {
  semesterId: Id
  name: string
  professor?: string | null
  credit: number // 1–6
  notionUrl?: string | null // 노션 필기 페이지 (≤1000)
  grade?: Grade | null // 4.5 만점 등급 (수정 요청에서 빠지면 서버가 지우므로 항상 보낸다)
  memo?: string | null // 과목 메모 (≤5000)
}
export type Grade = 'A+' | 'A0' | 'B+' | 'B0' | 'C+' | 'C0' | 'D+' | 'D0' | 'F'
export type Course = CourseRequest &
  Timestamps & { professor: string | null; notionUrl: string | null; grade: Grade | null; memo: string | null }

export interface AssignmentRequest {
  courseId: Id
  title: string
  dueDate: LocalDate
  completed: boolean
  notes?: string | null
}
export type Assignment = AssignmentRequest & Timestamps & { notes: string | null }

// ---- Study ----
export interface StudyTopicRequest {
  name: string
  description?: string | null
  notionUrl?: string | null // 노션 필기 페이지 (≤1000)
}
export type StudyTopic = StudyTopicRequest & Timestamps & { description: string | null; notionUrl: string | null }

export interface StudyProgressRequest {
  topicId: Id
  studiedAt: LocalDate
  minutes: number // 1–1440
  notes?: string | null
}
export type StudyProgress = StudyProgressRequest & Timestamps & { notes: string | null }

// ---- Life ----
export interface HabitRequest {
  name: string
  description?: string | null
}
export type Habit = HabitRequest & Timestamps & { description: string | null }

export interface HabitLogRequest {
  habitId: Id
  performedAt: LocalDate
  completed: boolean
  notes?: string | null
}
export type HabitLog = HabitLogRequest & Timestamps & { notes: string | null }

export interface ReadingLogRequest {
  title: string
  author?: string | null
  startedAt: LocalDate
  finishedAt?: LocalDate | null
  rating?: number | null // 1–5
  notes?: string | null
}
export type ReadingLog = Required<ReadingLogRequest> & Timestamps

// ---- Health ----
export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK'
export const MEAL_TYPES: MealType[] = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK']
export const MEAL_TYPE_KO: Record<MealType, string> = {
  BREAKFAST: '아침',
  LUNCH: '점심',
  DINNER: '저녁',
  SNACK: '간식',
}

export interface MealTotals {
  calories: number
  carbsG: number
  proteinG: number
  fatG: number
  sodiumMg: number
}

export interface MealItemRequest {
  mealRecordId: Id
  name: string
  calories: number
  carbsG?: number | null
  proteinG?: number | null
  fatG?: number | null
  sodiumMg?: number | null
}
export type MealItem = Required<MealItemRequest> & Timestamps

export interface MealRecordRequest {
  consumedAt: LocalDateTime
  mealType: MealType
  notes?: string | null
}
export interface MealRecord extends Timestamps {
  consumedAt: LocalDateTime
  mealType: MealType
  notes: string | null
  items: MealItem[]
  totals: MealTotals
}

export interface DailyMealSummary {
  date: LocalDate
  totals: MealTotals
  meals: { mealType: MealType; itemCount: number; totals: MealTotals }[]
}

export interface WorkoutLogRequest {
  performedAt: LocalDate
  type: string
  durationMinutes: number
  caloriesBurned?: number | null
  notes?: string | null
}
export type WorkoutLog = Required<WorkoutLogRequest> & Timestamps

export interface HealthLogRequest {
  recordedAt: LocalDateTime // 날짜+시각 (이슈 #137)
  weightKg?: number | null
  sleepHours?: number | null // 0–24
  notes?: string | null
}
export type HealthLog = Required<HealthLogRequest> & Timestamps

export type GoalRule = 'AT_MOST' | 'AT_LEAST'

/** 식단 목표. 값이 null이면 그 항목은 목표 없음 (사용자가 건강 화면에서 정함) */
export interface DietGoalRequest {
  carbsG: number | null
  carbsRule: GoalRule
  fatG: number | null
  fatRule: GoalRule
  proteinG: number | null
  proteinRule: GoalRule
  calories: number | null
  caloriesRule: GoalRule
}
export type DietGoal = DietGoalRequest & Timestamps

// ---- Schedule ----
export interface EventRequest {
  title: string
  startAt: LocalDateTime
  endAt: LocalDateTime // > startAt
  location?: string | null
  description?: string | null
  allDay: boolean
}
export type ScheduleEvent = Required<EventRequest> & Timestamps

// ---- Memo ----
export interface MemoRequest {
  title: string
  content: string // ≤5000
  tags?: string | null // 쉼표 구분, ≤300
}
export type Memo = Required<MemoRequest> & Timestamps

// ---- Hub ----
export interface HubCategoryRequest {
  name: string
  description?: string | null
}
export type HubCategory = Required<HubCategoryRequest> & Timestamps

export interface HubLinkRequest {
  categoryId: Id
  title: string
  url: string
  description?: string | null
}
export type HubLink = Required<HubLinkRequest> & Timestamps

// ---- Reminder ----
export interface ReminderRequest {
  title: string
  targetAt: LocalDateTime
  targetDomain?: string | null // ≤50
  targetEntityId?: Id | null
  sent: boolean
}
export type Reminder = Required<ReminderRequest> & Timestamps

// ---- User ----
export interface UserProfileRequest {
  displayName: string
  email?: string | null
  bio?: string | null
}
export type UserProfile = Required<UserProfileRequest> & Timestamps

export interface UserSettingRequest {
  theme: string
  language: string
  notificationEnabled: boolean
}
export type UserSetting = UserSettingRequest & Timestamps

// ---- 주간 통계 ----
export interface BatchRunResult {
  jobExecutionId: number
  status: string
  weekStart: LocalDate
}

export interface MealWeeklyStat extends Timestamps {
  weekStart: LocalDate
  dayCount: number
  avgCalories: number | null
  avgCarbsG: number | null
  avgProteinG: number | null
  avgFatG: number | null
}

export interface HealthLogWeeklyStat extends Timestamps {
  weekStart: LocalDate
  logCount: number
  avgWeightKg: number | null
  avgSleepHours: number | null
}

export interface StudyTopicWeeklyStat extends Timestamps {
  topicId: Id
  weekStart: LocalDate
  sessionCount: number
  totalMinutes: number
}

export interface HabitWeeklyStat extends Timestamps {
  habitId: Id
  weekStart: LocalDate
  totalCount: number
  completedCount: number
}

export interface AssignmentWeeklyStat extends Timestamps {
  courseId: Id
  weekStart: LocalDate
  totalCount: number
  completedCount: number
  completionRate: number // 0–1
}
