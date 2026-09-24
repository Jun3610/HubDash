import { useQueries } from '@tanstack/react-query'
import { useMemo } from 'react'
import { courses, semesters } from '../api/pknu'
import { BIG_PAGE, listKey, useList } from '../api/resource'
import type { Course, Id, Semester } from '../api/types'
import { inRange, type LocalDate } from '../lib/date'
import { useToday } from './useToday'

/** 오늘이 기간에 든 학기, 없으면 가장 최근에 시작한 학기 */
export function pickCurrentSemester(list: Semester[], today: LocalDate): Semester | null {
  const active = list.find((s) => inRange(today, s.startDate, s.endDate))
  if (active) return active
  return [...list].sort((a, b) => (a.startDate < b.startDate ? 1 : -1))[0] ?? null
}

export function useSemesters() {
  return useList(semesters, { size: BIG_PAGE, sort: 'startDate,desc' })
}

export function useCourses(semesterId: Id | null | undefined) {
  return useList(courses, { semesterId: semesterId ?? undefined, size: BIG_PAGE }, { enabled: semesterId != null })
}

/** 학기 → 과목을 한 번에. semesterId를 주면 그 학기, 없으면 현재 학기 (과제는 화면에서 뺐다, 이슈 #135) */
export function useSemesterBundle(semesterId?: Id | null) {
  const today = useToday()
  const sems = useSemesters()
  const list = sems.data?.content ?? []
  const semester =
    semesterId != null ? (list.find((s) => s.id === semesterId) ?? null) : pickCurrentSemester(list, today)
  const crs = useCourses(semester?.id)
  const courseList = useMemo(() => crs.data?.content ?? [], [crs.data])
  return {
    semesters: list,
    semester,
    courses: courseList,
    isLoading: sems.isLoading || (semester != null && crs.isLoading),
    error: sems.error ?? crs.error,
    refetch: () => {
      void sems.refetch()
      void crs.refetch()
    },
  }
}

/** 과목 색 점: 과목 순서대로 --course-1..6 */
export function courseColor(index: number): string {
  return `var(--course-${(index % 6) + 1})`
}

/** 모든 학기의 과목 (전체 평점 계산용). useCourses와 같은 쿼리 키라 캐시를 같이 쓴다 */
export function useAllCourses() {
  const sems = useSemesters()
  const list = useMemo(() => sems.data?.content ?? [], [sems.data])
  const results = useQueries({
    queries: list.map((sem) => {
      const q = { semesterId: sem.id, size: BIG_PAGE }
      return { queryKey: listKey(courses.path, q), queryFn: () => courses.list(q) }
    }),
  })
  const all: Course[] = results.flatMap((r) => r.data?.content ?? [])
  return {
    semesters: list,
    courses: all,
    isLoading: sems.isLoading || results.some((r) => r.isLoading),
    error: sems.error ?? results.find((r) => r.error)?.error ?? null,
  }
}
