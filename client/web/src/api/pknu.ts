import { defineResource } from './resource'
import type { Assignment, AssignmentRequest, Course, CourseRequest, Semester, SemesterRequest } from './types'

export const semesters = defineResource<Semester, SemesterRequest>('/api/pknu/semesters')
/** 목록 필수 쿼리: semesterId */
export const courses = defineResource<Course, CourseRequest>('/api/pknu/courses')
/** 목록 필수 쿼리: courseId */
export const assignments = defineResource<Assignment, AssignmentRequest>('/api/pknu/assignments')

/**
 * 과목 수정 요청 본문. 서버 PUT은 빠진 필드를 null로 지우므로 기존 값을 모두 담고 바꿀 것만 덮어쓴다.
 * (notionUrl·grade·memo가 한 번씩 지워질 뻔했던 문제, 이슈 #134·#135)
 */
export function courseBody(c: Course, patch: Partial<CourseRequest> = {}): CourseRequest {
  return {
    semesterId: c.semesterId,
    name: c.name,
    professor: c.professor,
    credit: c.credit,
    notionUrl: c.notionUrl,
    grade: c.grade,
    memo: c.memo,
    ...patch,
  }
}
