import { Plus } from 'lucide-react'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { mealRecords } from '../api/health'
import { useList } from '../api/resource'
import type { HealthLog, WorkoutLog } from '../api/types'
import { PageHeader } from '../components/layout/PageHeader'
import { Button, DateNav, Tabs } from '../components/ui'
import { YEAR_PAGE } from '../hooks/useActivity'
import { useToday } from '../hooks/useToday'
import { useUrlState } from '../hooks/useUrlState'
import { shiftDate, weekStartOf } from '../lib/date'
import { withinDates, withinDateTimes } from '../lib/select/range'
import { useBodyLogs, useMealStats, useWorkouts } from './health/data'
import s from './health/Health.module.css'
import { DailySummaryCard, MealCards, MealRecordModal } from './health/MealSection'
import {
  BodyAsideCard,
  BodyModal,
  BodyTab,
  StatsTab,
  WeeklyCaloriesCard,
  WorkoutAsideCard,
  WorkoutModal,
  WorkoutTab,
} from './health/OtherSections'

type Tab = 'meal' | 'workout' | 'body' | 'stats'
type Dialog = { kind: 'meal' } | { kind: 'workout'; log?: WorkoutLog } | { kind: 'body'; log?: HealthLog } | null

export default function HealthPage() {
  const today = useToday()
  const [tab, setTab] = useUrlState('tab', 'meal')
  const [date, setDate] = useUrlState('date', today)
  const [dialog, setDialog] = useState<Dialog>(null)
  const [params, setParams] = useSearchParams()

  // 빠른 기록(?new=meal)으로 들어오면 해당 추가 창을 연다 (주소에서 바로 계산)
  const newKind = params.get('new')
  const active: Dialog =
    dialog ?? (newKind === 'meal' || newKind === 'workout' || newKind === 'body' ? ({ kind: newKind } as Dialog) : null)
  const closeDialog = () => {
    setDialog(null)
    if (newKind)
      setParams(
        (p) => {
          const n = new URLSearchParams(p)
          n.delete('new')
          return n
        },
        { replace: true },
      )
  }

  // 끼니 목록은 기록 히트맵과 같은 쿼리를 써서 캐시를 공유한다 (날짜 필터가 없어 넉넉히 받는다)
  const meals = useList(mealRecords, { size: YEAR_PAGE, sort: 'consumedAt,desc' })
  const dayMeals = withinDateTimes(meals.data?.content ?? [], (r) => r.consumedAt, date, date)
  const workouts = useWorkouts()
  const body = useBodyLogs()
  const statsCount = useMealStats().data?.totalElements
  const from = weekStartOf(today)

  return (
    <>
      <PageHeader
        title="건강"
        tabs={
          <Tabs<Tab>
            inHeader
            label="건강 탭"
            value={tab as Tab}
            onChange={setTab}
            items={[
              { key: 'meal', label: '식단', count: dayMeals.length },
              {
                key: 'workout',
                label: '운동',
                count: withinDates(workouts.data?.content ?? [], (w) => w.performedAt, from, shiftDate(from, 6)).length,
              },
              { key: 'body', label: '체중 · 수면', count: body.data?.totalElements },
              { key: 'stats', label: '주간 통계', count: statsCount },
            ]}
          />
        }
      >
        {tab === 'meal' && <DateNav value={date} onChange={setDate} today={today} />}
        <Button
          variant="primary"
          icon={<Plus size={14} />}
          onClick={() => setDialog({ kind: tab === 'workout' ? 'workout' : tab === 'body' ? 'body' : 'meal' })}
        >
          {tab === 'workout' ? '운동 추가' : tab === 'body' ? '기록 추가' : '끼니 추가'}
        </Button>
      </PageHeader>

      <div className={s.layout}>
        <section className={s.main} aria-label="건강 본문">
          {tab === 'meal' && (
            <>
              <DailySummaryCard date={date} />
              <MealCards
                date={date}
                today={today}
                records={dayMeals}
                loading={meals.isLoading}
                error={meals.error}
                onRetry={() => void meals.refetch()}
              />
            </>
          )}
          {tab === 'workout' && (
            <WorkoutTab
              onAdd={() => setDialog({ kind: 'workout' })}
              onEdit={(log) => setDialog({ kind: 'workout', log })}
            />
          )}
          {tab === 'body' && (
            <BodyTab onAdd={() => setDialog({ kind: 'body' })} onEdit={(log) => setDialog({ kind: 'body', log })} />
          )}
          {tab === 'stats' && <StatsTab />}
        </section>
        <aside className={s.aside} aria-label="건강 요약">
          <WeeklyCaloriesCard />
          <WorkoutAsideCard today={today} onAdd={() => setDialog({ kind: 'workout' })} />
          <BodyAsideCard onAdd={() => setDialog({ kind: 'body' })} />
        </aside>
      </div>

      {active?.kind === 'meal' && <MealRecordModal date={date} today={today} onClose={closeDialog} />}
      {active?.kind === 'workout' && <WorkoutModal today={today} log={active.log} onClose={closeDialog} />}
      {active?.kind === 'body' && <BodyModal today={today} log={active.log} onClose={closeDialog} />}
    </>
  )
}
