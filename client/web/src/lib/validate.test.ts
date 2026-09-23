import { describe, expect, it } from 'vitest'
import { eulReul, hasErrors, isUrl, maxLen, numRange, optNum, required } from './validate'

describe('validate', () => {
  it('required', () => {
    expect(required('  ')).toBeTruthy()
    expect(required(null)).toBeTruthy()
    expect(required('a')).toBeUndefined()
    expect(required(0)).toBeUndefined()
  })
  it('조사: 받침에 따라 을/를', () => {
    expect(eulReul('종류')).toBe('를')
    expect(eulReul('음식 이름')).toBe('을')
    expect(eulReul('kcal')).toBe('을(를)')
    expect(required('', '날짜')).toBe('날짜를 입력하세요')
  })
  it('numRange: 빈 값 통과, 범위·정수 검사', () => {
    expect(numRange('', 1, 6)).toBeUndefined()
    expect(numRange('7', 1, 6)).toBeTruthy()
    expect(numRange('abc', 1, 6)).toBeTruthy()
    expect(numRange('2.5', 1, 6, true)).toBeTruthy()
    expect(numRange('24', 0, 24)).toBeUndefined()
  })
  it('maxLen', () => {
    expect(maxLen('a'.repeat(5001), 5000)).toContain('5001')
    expect(maxLen(null, 3)).toBeUndefined()
  })
  it('isUrl', () => {
    expect(isUrl('https://github.com')).toBeUndefined()
    expect(isUrl('github.com')).toBeTruthy()
    expect(isUrl('ftp://x.y')).toBeTruthy()
  })
  it('optNum / hasErrors', () => {
    expect(optNum('')).toBeNull()
    expect(optNum('1.5')).toBe(1.5)
    expect(hasErrors({ a: undefined })).toBe(false)
    expect(hasErrors({ a: 'x' })).toBe(true)
  })
})
