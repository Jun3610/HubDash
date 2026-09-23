import { defineResource } from './resource'
import type { Assignment, AssignmentRequest, Course, CourseRequest, Semester, SemesterRequest } from './types'

export const semesters = defineResource<Semester, SemesterRequest>('/api/pknu/semesters')
/** 목록 필수 쿼리: semesterId */
export const courses = defineResource<Course, CourseRequest>('/api/pknu/courses')
/** 목록 필수 쿼리: courseId */
export const assignments = defineResource<Assignment, AssignmentRequest>('/api/pknu/assignments')
