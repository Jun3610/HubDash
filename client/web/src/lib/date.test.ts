import { describe, expect, it } from 'vitest'
import {
  daysUntil,
  ddayLabel,
  ddayTone,
  formatHeaderDate,
  formatMinutes,
  parseLocalDate,
  parseLocalDateTime,
  toLocalDate,
  weekDays,
  weekStartOf,
} from './date'

describe('parseLocalDate', () => {
  it('로컬 자정으로 읽어 날짜가 밀리지 않는다', () => {
    const d = parseLocalDate('2026-09-23')
    expect(d.getFullYear()).toBe(2026)
    expect(d.getMonth()).toBe(8)
    expect(d.getDate()).toBe(23)
    expect(d.getHours()).toBe(0)
    expect(toLocalDate(d)).toBe('2026-09-23')
  })

  it('형식이 틀리면 예외', () => {
    expect(() => parseLocalDate('2026/09/23')).toThrow()
  })
})

describe('parseLocalDateTime', () => {
  it.each([
    ['2026-09-23T15:00', 0],
    ['2026-09-23T15:00:00', 0],
    ['2026-09-23T15:00:00.1', 100],
    ['2026-09-23T15:00:00.123', 123],
    ['2026-09-23T15:00:00.123456', 123],
    ['2026-09-23T15:00:00.123456789', 123],
  ])('%s 를 소수 초 자릿수와 상관없이 읽는다', (value, ms) => {
    const d = parseLocalDateTime(value)
    expect(d.getHours()).toBe(15)
    expect(d.getDate()).toBe(23)
    expect(d.getMilliseconds()).toBe(ms)
  })

  it('시간대가 붙은 값은 거부한다', () => {
    expect(() => parseLocalDateTime('2026-09-23T15:00:00Z')).toThrow()
  })
})

describe('주 계산 (월요일 시작)', () => {
  it('수요일의 주 시작은 그 주 월요일', () => {
    expect(weekStartOf('2026-09-23')).toBe('2026-09-21')
  })
  it('일요일은 앞 주 월요일에 속한다', () => {
    expect(weekStartOf('2026-09-27')).toBe('2026-09-21')
  })
  it('월요일은 자기 자신', () => {
    expect(weekStartOf('2026-09-21')).toBe('2026-09-21')
  })
  it('월~일 7일', () => {
    expect(weekDays('2026-09-23')).toEqual([
      '2026-09-21',
      '2026-09-22',
      '2026-09-23',
      '2026-09-24',
      '2026-09-25',
      '2026-09-26',
      '2026-09-27',
    ])
  })
  it('연말을 넘어가도 맞다', () => {
    expect(weekStartOf('2027-01-01')).toBe('2026-12-28')
  })
})

describe('D-day', () => {
  it('남은 일수', () => {
    expect(daysUntil('2026-09-24', '2026-09-23')).toBe(1)
    expect(daysUntil('2026-09-23', '2026-09-23')).toBe(0)
    expect(daysUntil('2026-09-20', '2026-09-23')).toBe(-3)
    expect(daysUntil('2026-10-01', '2026-09-23')).toBe(8)
  })
  it('표기', () => {
    expect(ddayLabel(1)).toBe('D-1')
    expect(ddayLabel(0)).toBe('D-day')
    expect(ddayLabel(-2)).toBe('D+2')
  })
  it('색: D-1 이하 빨강, D-3 이하 노랑, 그 외 회색, 완료 보라', () => {
    expect(ddayTone(-1)).toBe('red')
    expect(ddayTone(0)).toBe('red')
    expect(ddayTone(1)).toBe('red')
    expect(ddayTone(2)).toBe('yellow')
    expect(ddayTone(3)).toBe('yellow')
    expect(ddayTone(4)).toBe('gray')
    expect(ddayTone(0, true)).toBe('purple')
  })
})

describe('표기', () => {
  it('헤더 날짜', () => {
    expect(formatHeaderDate('2026-09-23')).toBe('2026.09.23 수')
  })
  it('분', () => {
    expect(formatMinutes(400)).toBe('6h 40m')
    expect(formatMinutes(40)).toBe('40m')
    expect(formatMinutes(120)).toBe('2h')
  })
})
