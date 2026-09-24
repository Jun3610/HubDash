import { Dumbbell, Plus, Scale, Target, Utensils } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { mealRecords } from '../api/health'
import { useList } from '../api/resource'
import type { HealthLog, WorkoutLog } from '../api/types'
import { PageHeader } from '../components/layout/PageHeader'
import { Button, DateNav, Peek } from '../components/ui'
import { YEAR_PAGE } from '../hooks/useActivity'
import { useToday } from '../hooks/useToday'
import { formatHeaderDate, shiftDate, type LocalDate } from '../lib/date'
import { withinDateTimes } from '../lib/select/range'
import { DaySummary, GoalModal, MealCards } from './health/DietTab'
import s from './health/Health.module.css'
import { BodyModal, BodyTab, WorkoutModal, WorkoutTab } from './health/OtherSections'
import { HealthDashboard } from './health/Dashboard'

/** 작은 창 종류. 예전 탭 주소(?tab=meal 등)를 그대로 쓴다 */
type PeekKind = 'meal' | 'workout' | 'body'
type Dialog = { kind: 'goal' } | { kind: 'workout'; log?: WorkoutLog } | { kind: 'body'; log?: HealthLog } | null

const PEEKS: readonly string[] = ['meal', 'workout', 'body']

/** 건강: 탭 없이 대시보드 한 화면, 식단·운동·체중수면 기록은 작은 창으로 (이슈 #148) */
export default function HealthPage() {
  const today = useToday()
  const [params, setParams] = useSearchParams()
  const [dialog, setDialog] = useState<Dialog>(null)

  // 끼니 목록은 기록 히트맵과 같은 쿼리를 써서 캐시를 공유한다 (날짜 필터가 없어 넉넉히 받는다)
  const meals = useList(mealRecords, { size: YEAR_PAGE, sort: 'consumedAt,desc' })
  const all = meals.data?.content ?? []

  // 빠른 기록(?new=meal 등): 끼니는 오늘 식단 창, 운동·체중은 입력 창
  const newKind = params.get('new')
  const tabParam = newKind === 'meal' ? 'meal' : params.get('tab')
  const peek = tabParam && PEEKS.includes(tabParam) ? (tabParam as PeekKind) : null
  const date = (params.get('date') ?? today) as LocalDate
  const dayMeals = withinDateTimes(all, (r) => r.consumedAt, date, date)
  const active: Dialog = dialog ?? (newKind === 'workout' || newKind === 'body' ? ({ kind: newKind } as Dialog) : null)

  const update = useCallback(
    (fn: (p: URLSearchParams) => void, replace = false) =>
      setParams(
        (p) => {
          const n = new URLSearchParams(p)
          fn(n)
          return n
        },
        { replace },
      ),
    [setParams],
  )
  const openPeek = (kind: PeekKind, d?: LocalDate) =>
    update((n) => {
      n.set('tab', kind)
      if (d && d !== today) n.set('date', d)
      else n.delete('date')
    })
  const closePeek = useCallback(
    () =>
      update((n) => {
        n.delete('tab')
        n.delete('date')
        n.delete('new')
      }, true),
    [update],
  )
  const setDate = (d: LocalDate) => update((n) => (d === today ? n.delete('date') : n.set('date', d)), true)
  // 식단 창에서 ←/→로 하루씩 (이슈 #195). 입력 중이거나 끼니 입력 창이 위에 떠 있으면 두지 않는다
  const dateRef = useRef({ date, setDate })
  useEffect(() => {
    dateRef.current = { date, setDate }
  })
  useEffect(() => {
    if (peek !== 'meal') return
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return
      if (e.metaKey || e.ctrlKey || e.altKey || e.shiftKey) return
      const el = e.target as HTMLElement
      if (el.closest('input, textarea, select, [contenteditable="true"]')) return
      if (document.querySelectorAll('[role="dialog"]').length > 1) return
      e.preventDefault()
      const { date: d, setDate: go } = dateRef.current
      go(shiftDate(d, e.key === 'ArrowLeft' ? -1 : 1))
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [peek])
  const closeDialog = () => {
    setDialog(null)
    if (newKind === 'workout' || newKind === 'body') update((n) => n.delete('new'), true)
  }

  return (
    <>
      <PageHeader title="Health">
        <Button icon={<Target size={14} />} onClick={() => setDialog({ kind: 'goal' })}>
          Diet Goal
        </Button>
        <Button icon={<Scale size={14} />} onClick={() => setDialog({ kind: 'body' })}>
          Weight · Sleep
        </Button>
        <Button icon={<Dumbbell size={14} />} onClick={() => setDialog({ kind: 'workout' })}>
          Workout
        </Button>
        <Button variant="primary" icon={<Plus size={14} />} onClick={() => openPeek('meal')}>
          Add Meal
        </Button>
      </PageHeader>

      <div className={s.page}>
        <HealthDashboard
          records={all}
          loading={meals.isLoading}
          today={today}
          onGoals={() => setDialog({ kind: 'goal' })}
          onOpen={openPeek}
        />
      </div>

      {peek === 'meal' && (
        <Peek label={`${formatHeaderDate(date)} 식단`} onClose={closePeek}>
          <div className={s.peekHead}>
            <Utensils size={18} strokeWidth={1.8} aria-hidden="true" />
            <h1>식단</h1>
            <DateNav value={date} onChange={setDate} today={today} />
          </div>
          <DaySummary records={dayMeals} date={date} onGoals={() => setDialog({ kind: 'goal' })} />
          <MealCards
            records={dayMeals}
            date={date}
            today={today}
            loading={meals.isLoading}
            error={meals.error}
            onRetry={() => void meals.refetch()}
          />
        </Peek>
      )}
      {peek === 'workout' && (
        <Peek label="운동 기록" onClose={closePeek}>
          <WorkoutTab
            onAdd={() => setDialog({ kind: 'workout' })}
            onEdit={(log) => setDialog({ kind: 'workout', log })}
          />
        </Peek>
      )}
      {peek === 'body' && (
        <Peek label="체중 · 수면 기록" onClose={closePeek}>
          <BodyTab onAdd={() => setDialog({ kind: 'body' })} onEdit={(log) => setDialog({ kind: 'body', log })} />
        </Peek>
      )}

      {active?.kind === 'goal' && <GoalModal onClose={closeDialog} />}
      {active?.kind === 'workout' && <WorkoutModal today={today} log={active.log} onClose={closeDialog} />}
      {active?.kind === 'body' && <BodyModal today={today} log={active.log} onClose={closeDialog} />}
    </>
  )
}
