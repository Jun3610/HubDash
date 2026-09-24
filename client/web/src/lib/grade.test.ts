import { describe, expect, it } from 'vitest'
import { formatGpa, GRADE_POINT, gpaOf } from './grade'

describe('평점 (4.5 만점)', () => {
  it('등급별 점수: A+ 4.5부터 0.5씩, D0 1.0, F 0', () => {
    expect(GRADE_POINT['A+']).toBe(4.5)
    expect(GRADE_POINT.A0).toBe(4.0)
    expect(GRADE_POINT['B+']).toBe(3.5)
    expect(GRADE_POINT['C+']).toBe(2.5)
    expect(GRADE_POINT['D+']).toBe(1.5)
    expect(GRADE_POINT.D0).toBe(1.0)
    expect(GRADE_POINT.F).toBe(0)
  })
  it('학점 가중 평균, 성적 없는 과목은 평균에서 뺀다', () => {
    const r = gpaOf([
      { credit: 3, grade: 'A+' },
      { credit: 3, grade: 'B0' },
      { credit: 2, grade: 'C+' },
      { credit: 3, grade: null },
    ])
    // (3×4.5 + 3×3.0 + 2×2.5) / 8 = 27.5 / 8 = 3.4375
    expect(r.gpa).toBe(3.44)
    expect(r.gradedCredits).toBe(8)
    expect(r.totalCredits).toBe(11)
    expect(r.earnedCredits).toBe(8)
  })
  it('F는 평균에 0점으로 들어가고 이수 학점에서 빠진다', () => {
    const r = gpaOf([
      { credit: 3, grade: 'A0' },
      { credit: 3, grade: 'F' },
    ])
    expect(r.gpa).toBe(2)
    expect(r.earnedCredits).toBe(3)
  })
  it('성적이 하나도 없으면 null', () => {
    expect(gpaOf([{ credit: 3, grade: null }]).gpa).toBeNull()
    expect(formatGpa(null)).toBe('—')
    expect(formatGpa(3.5)).toBe('3.50')
  })
})
