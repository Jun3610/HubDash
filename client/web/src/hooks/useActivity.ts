import { useMemo } from 'react'
import { healthLogs, mealRecords, workoutLogs } from '../api/health'
import { events } from '../api/schedule'
import { habitLogs, habits } from '../api/life'
import { BIG_PAGE, useList, useListsByParent } from '../api/resource'
import { studyProgresses, studyTopics } from '../api/study'
import type { HabitLog, StudyProgress } from '../api/types'
import { datePart, type LocalDate } from '../lib/date'
import { countByDate } from '../lib/heatmap'
import { useToday } from './useToday'
import type { ActivitySources } from '../lib/select/activity'

/** 1년치 기록을 모으기 위한 큰 페이지 (Spring 기본 최대 2000) */
export const YEAR_PAGE = 2000

export type ActivityDomain = 'meal' | 'study' | 'habit' | 'workout' | 'body' | 'event'

export const ACTIVITY_LABEL: Record<ActivityDomain, string> = {
  meal: '식단',
  study: '공부',
  habit: '습관',
  workout: '운동',
  body: '체중·수면',
  event: '일정',
}

/**
 * 기록 히트맵용: 끼니, 운동, 체중·수면, 공부, 습관(완료), 지난 일정 날짜를 모아 날짜별 건수로 합산한다.
 * 일정은 시작일이 오늘 이전인 것만 센다(앞으로 할 일은 기록이 아님, 이슈 #133).
 */
export function useActivity() {
  const meals = useList(mealRecords, { size: YEAR_PAGE, sort: 'consumedAt,desc' })
  const workouts = useList(workoutLogs, { size: YEAR_PAGE, sort: 'performedAt,desc' })
  const today = useToday()
  // useAllEvents와 같은 조건이라 캐시를 같이 쓴다
  const evs = useList(events, { size: YEAR_PAGE, sort: 'startAt,desc' })
  const body = useList(healthLogs, { size: YEAR_PAGE, sort: 'recordedAt,desc' })

  const topics = useList(studyTopics, { size: BIG_PAGE })
  const topicIds = useMemo(() => (topics.data?.content ?? []).map((t) => t.id), [topics.data])
  const progresses = useListsByParent<StudyProgress>(studyProgresses, 'topicId', topicIds, {
    size: YEAR_PAGE,
    sort: 'studiedAt,desc',
  })

  const habitList = useList(habits, { size: BIG_PAGE })
  const habitIds = useMemo(() => (habitList.data?.content ?? []).map((h) => h.id), [habitList.data])
  const logs = useListsByParent<HabitLog>(habitLogs, 'habitId', habitIds, { size: YEAR_PAGE, sort: 'performedAt,desc' })

  const byDomain = useMemo(() => {
    const d: Record<ActivityDomain, LocalDate[]> = {
      meal: (meals.data?.content ?? []).map((m) => datePart(m.consumedAt)),
      workout: (workouts.data?.content ?? []).map((w) => w.performedAt),
      body: (body.data?.content ?? []).map((b) => datePart(b.recordedAt)),
      study: progresses.data.map((p) => p.studiedAt),
      habit: logs.data.filter((l) => l.completed).map((l) => l.performedAt),
      event: (evs.data?.content ?? []).map((e) => datePart(e.startAt)).filter((d) => d <= today),
    }
    return d
  }, [meals.data, workouts.data, body.data, progresses.data, logs.data, evs.data, today])

  const counts = useMemo(() => countByDate(...Object.values(byDomain)), [byDomain])

  // 날짜별 상세 목록용 원본 (사이드바 날짜 클릭, 이슈 #132)
  const sources = useMemo<ActivitySources>(
    () => ({
      meals: meals.data?.content ?? [],
      workouts: workouts.data?.content ?? [],
      body: body.data?.content ?? [],
      progresses: progresses.data,
      topics: topics.data?.content ?? [],
      habitLogs: logs.data,
      habits: habitList.data?.content ?? [],
      events: evs.data?.content ?? [],
    }),
    [meals.data, workouts.data, body.data, progresses.data, topics.data, logs.data, habitList.data, evs.data],
  )

  return {
    counts,
    byDomain,
    sources,
    isLoading:
      meals.isLoading ||
      evs.isLoading ||
      workouts.isLoading ||
      body.isLoading ||
      topics.isLoading ||
      progresses.isLoading ||
      habitList.isLoading ||
      logs.isLoading,
    error:
      meals.error ??
      evs.error ??
      workouts.error ??
      body.error ??
      topics.error ??
      progresses.error ??
      habitList.error ??
      logs.error,
  }
}
