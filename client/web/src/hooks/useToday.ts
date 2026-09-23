import { useEffect, useState } from 'react'
import { todayLocalDate, type LocalDate } from '../lib/date'

/** 오늘 날짜. 자정이 지나면 1분 안에 갱신된다 */
export function useToday(): LocalDate {
  const [today, setToday] = useState(todayLocalDate)
  useEffect(() => {
    const t = setInterval(() => setToday(todayLocalDate()), 60_000)
    return () => clearInterval(t)
  }, [])
  return today
}

/** 현재 시각 (분 단위 갱신) — 일정 현재 시각선 등 */
export function useNow(intervalMs = 60_000): Date {
  const [now, setNow] = useState(() => new Date())
  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), intervalMs)
    return () => clearInterval(t)
  }, [intervalMs])
  return now
}
