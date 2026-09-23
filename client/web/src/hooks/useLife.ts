import { useMemo } from 'react'
import { habitLogs, habits } from '../api/life'
import { BIG_PAGE, useCreate, useList, useListsByParent, useUpdate } from '../api/resource'
import type { Habit, HabitLog, Id } from '../api/types'
import type { LocalDate } from '../lib/date'

export function useHabits() {
  return useList(habits, { size: BIG_PAGE, sort: 'createdAt,asc' })
}

/** 습관 + 습관별 기록(최근 것부터) */
export function useHabitsWithLogs() {
  const list = useHabits()
  const items = useMemo(() => list.data?.content ?? [], [list.data])
  const ids = useMemo(() => items.map((h) => h.id), [items])
  const logs = useListsByParent<HabitLog>(habitLogs, 'habitId', ids, { size: BIG_PAGE, sort: 'performedAt,desc' })
  return {
    habits: items,
    logsByHabit: logs.byParent,
    isLoading: list.isLoading || logs.isLoading,
    error: list.error ?? logs.error,
    refetch: () => {
      void list.refetch()
      void logs.refetch()
    },
  }
}

/** 그 날짜의 기록 (같은 날 여러 건이면 완료된 것 우선) */
export function logOn(logs: HabitLog[] | undefined, date: LocalDate): HabitLog | undefined {
  const same = (logs ?? []).filter((l) => l.performedAt === date)
  return same.find((l) => l.completed) ?? same[0]
}

/** 체크 토글: 그날 기록이 있으면 completed를 뒤집고, 없으면 완료 기록을 만든다 */
export function useToggleHabit() {
  const create = useCreate(habitLogs)
  const update = useUpdate(habitLogs)
  return {
    isPending: create.isPending || update.isPending,
    toggle: (habit: Habit, logs: HabitLog[] | undefined, date: LocalDate, opts?: { onSettled?: () => void }) => {
      const log = logOn(logs, date)
      if (log) {
        update.mutate(
          {
            id: log.id,
            body: { habitId: habit.id, performedAt: log.performedAt, completed: !log.completed, notes: log.notes },
          },
          opts,
        )
      } else {
        create.mutate({ habitId: habit.id as Id, performedAt: date, completed: true, notes: null }, opts)
      }
    },
  }
}

/** 오늘부터 거꾸로 완료 연속 일수 (오늘 미완료면 어제부터) */
export function habitStreak(
  logs: HabitLog[] | undefined,
  today: LocalDate,
  shift: (d: LocalDate, n: number) => LocalDate,
) {
  const done = new Set((logs ?? []).filter((l) => l.completed).map((l) => l.performedAt))
  let day = done.has(today) ? today : shift(today, -1)
  let n = 0
  while (done.has(day)) {
    n++
    day = shift(day, -1)
  }
  return n
}
