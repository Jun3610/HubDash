import { addDays, differenceInCalendarDays, format } from 'date-fns'

/** 서버 LocalDate. "2026-09-23" 문자열 그대로 다룬다 */
export type LocalDate = string
/** 서버 LocalDateTime. "2026-09-23T15:00:00(.fffffffff)" — 시간대 없음 */
export type LocalDateTime = string

const LOCAL_DATE = /^(\d{4})-(\d{2})-(\d{2})$/
const LOCAL_DATE_TIME = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,9}))?)?$/

/**
 * LocalDate를 로컬 자정 Date로 읽는다.
 * new Date("2026-09-23")은 UTC로 읽혀 한국 시간 기준 날짜가 밀릴 수 있어 쓰지 않는다.
 */
export function parseLocalDate(value: LocalDate): Date {
  const m = LOCAL_DATE.exec(value)
  if (!m) throw new Error(`LocalDate 형식이 아닙니다: ${value}`)
  return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
}

/** LocalDateTime을 로컬 시각 Date로 읽는다. 소수 초 자릿수(0~9)에 상관없이 읽는다 */
export function parseLocalDateTime(value: LocalDateTime): Date {
  const m = LOCAL_DATE_TIME.exec(value)
  if (!m) throw new Error(`LocalDateTime 형식이 아닙니다: ${value}`)
  const ms = m[7] ? Math.floor(Number(m[7].padEnd(9, '0')) / 1_000_000) : 0
  return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]), Number(m[4]), Number(m[5]), m[6] ? Number(m[6]) : 0, ms)
}

export function toLocalDate(date: Date): LocalDate {
  return format(date, 'yyyy-MM-dd')
}

export function toLocalDateTime(date: Date): LocalDateTime {
  return format(date, "yyyy-MM-dd'T'HH:mm:ss")
}

/** LocalDateTime에서 날짜 부분만 */
export function datePart(value: LocalDateTime): LocalDate {
  return value.slice(0, 10)
}

export function todayLocalDate(now = new Date()): LocalDate {
  return toLocalDate(now)
}

export function shiftDate(value: LocalDate, days: number): LocalDate {
  return toLocalDate(addDays(parseLocalDate(value), days))
}

/** from에서 to까지 며칠 (같은 날 0) */
export function daysBetween(from: LocalDate, to: LocalDate): number {
  return differenceInCalendarDays(parseLocalDate(to), parseLocalDate(from))
}

/** 서버 주간 통계와 같은 기준: 월요일 시작 */
export function weekStartOf(value: LocalDate): LocalDate {
  const d = parseLocalDate(value)
  const offset = (d.getDay() + 6) % 7 // 월=0 … 일=6
  return toLocalDate(addDays(d, -offset))
}

/** 해당 주 월~일 7개 날짜 */
export function weekDays(value: LocalDate): LocalDate[] {
  const start = weekStartOf(value)
  return Array.from({ length: 7 }, (_, i) => shiftDate(start, i))
}

/** from <= value <= to (문자열 비교가 날짜 비교와 같다) */
export function inRange(value: LocalDate, from: LocalDate, to: LocalDate): boolean {
  return value >= from && value <= to
}

const WEEKDAY_KO = ['일', '월', '화', '수', '목', '금', '토']

export function weekdayKo(value: LocalDate): string {
  return WEEKDAY_KO[parseLocalDate(value).getDay()]
}

/** 헤더 표기 "2026.09.23 수" */
export function formatHeaderDate(value: LocalDate): string {
  return `${value.replaceAll('-', '.')} ${weekdayKo(value)}`
}

/** "09.23" */
export function formatShortDate(value: LocalDate): string {
  return value.slice(5).replace('-', '.')
}

/** LocalDateTime → "15:00" */
export function formatTime(value: LocalDateTime): string {
  return value.slice(11, 16)
}

// ---- D-day ----

/** 마감일까지 남은 달력 일수. 오늘 마감이면 0, 지났으면 음수 */
export function daysUntil(due: LocalDate, today: LocalDate): number {
  return differenceInCalendarDays(parseLocalDate(due), parseLocalDate(today))
}

export function ddayLabel(days: number): string {
  if (days === 0) return 'D-day'
  return days > 0 ? `D-${days}` : `D+${-days}`
}

export type DdayTone = 'red' | 'yellow' | 'gray' | 'purple'

/** D-1 이하(오늘·지남 포함) 빨강, D-3 이하 노랑, 그 외 회색, 완료는 보라 */
export function ddayTone(days: number, completed = false): DdayTone {
  if (completed) return 'purple'
  if (days <= 1) return 'red'
  if (days <= 3) return 'yellow'
  return 'gray'
}

/** 분 → "6h 40m" / "40m" */
export function formatMinutes(total: number): string {
  const h = Math.floor(total / 60)
  const m = total % 60
  if (h === 0) return `${m}m`
  return m === 0 ? `${h}h` : `${h}h ${m}m`
}
