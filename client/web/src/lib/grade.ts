import type { Course, Grade } from '../api/types'

/** 4.5 만점: A+ 4.5부터 0.5씩 내려가고 D0 1.0, F 0 (사용자 기준, 이슈 #134) */
export const GRADES: Grade[] = ['A+', 'A0', 'B+', 'B0', 'C+', 'C0', 'D+', 'D0', 'F']

export const GRADE_POINT: Record<Grade, number> = {
  'A+': 4.5,
  A0: 4.0,
  'B+': 3.5,
  B0: 3.0,
  'C+': 2.5,
  C0: 2.0,
  'D+': 1.5,
  D0: 1.0,
  F: 0,
}

export interface GpaResult {
  /** 성적이 나온 과목의 학점 가중 평균. 성적 나온 과목이 없으면 null */
  gpa: number | null
  /** 성적이 나온 과목 학점 합 */
  gradedCredits: number
  /** 전체 학점 합 (성적 안 나온 과목 포함) */
  totalCredits: number
  /** F를 뺀 이수 학점 */
  earnedCredits: number
}

export function gpaOf(courses: Pick<Course, 'credit' | 'grade'>[]): GpaResult {
  let points = 0
  let gradedCredits = 0
  let totalCredits = 0
  let earnedCredits = 0
  for (const c of courses) {
    totalCredits += c.credit
    if (!c.grade) continue
    gradedCredits += c.credit
    points += c.credit * GRADE_POINT[c.grade]
    if (c.grade !== 'F') earnedCredits += c.credit
  }
  return {
    gpa: gradedCredits ? Math.round((points / gradedCredits) * 100) / 100 : null,
    gradedCredits,
    totalCredits,
    earnedCredits,
  }
}

/** 3.83 → "3.83", null → "—" */
export function formatGpa(gpa: number | null): string {
  return gpa === null ? '—' : gpa.toFixed(2)
}

export function gradeTone(grade: Grade | null): 'purple' | 'green' | 'blue' | 'yellow' | 'orange' | 'red' | 'gray' {
  if (!grade) return 'gray'
  if (grade.startsWith('A')) return 'purple'
  if (grade.startsWith('B')) return 'green'
  if (grade.startsWith('C')) return 'blue'
  if (grade.startsWith('D')) return 'orange'
  return 'red'
}
