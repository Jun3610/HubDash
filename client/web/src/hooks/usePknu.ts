import { useMemo } from 'react'
import { assignments, courses, semesters } from '../api/pknu'
import { BIG_PAGE, useList, useListsByParent } from '../api/resource'
import type { Assignment, Course, Id, Semester } from '../api/types'
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

/** 과목별 과제를 병렬 조회해 합친다 (전체 과제 API가 없다) */
export function useAssignmentsFor(courseList: Course[]) {
  const ids = useMemo(() => courseList.map((c) => c.id), [courseList])
  return useListsByParent<Assignment>(assignments, 'courseId', ids, { size: BIG_PAGE, sort: 'dueDate,asc' })
}

/** 학기 → 과목 → 과제를 한 번에. semesterId를 주면 그 학기, 없으면 현재 학기 */
export function useSemesterBundle(semesterId?: Id | null) {
  const today = useToday()
  const sems = useSemesters()
  const list = sems.data?.content ?? []
  const semester =
    semesterId != null ? (list.find((s) => s.id === semesterId) ?? null) : pickCurrentSemester(list, today)
  const crs = useCourses(semester?.id)
  const courseList = useMemo(() => crs.data?.content ?? [], [crs.data])
  const asg = useAssignmentsFor(courseList)
  return {
    semesters: list,
    semester,
    courses: courseList,
    assignments: asg.data,
    assignmentsByCourse: asg.byParent,
    isLoading: sems.isLoading || (semester != null && crs.isLoading) || asg.isLoading,
    error: sems.error ?? crs.error ?? asg.error,
    refetch: () => {
      void sems.refetch()
      void crs.refetch()
      void asg.refetch()
    },
  }
}

/** 과목 색 점: 과목 순서대로 --course-1..6 */
export function courseColor(index: number): string {
  return `var(--course-${(index % 6) + 1})`
}
