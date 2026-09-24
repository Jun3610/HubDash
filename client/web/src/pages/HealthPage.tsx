import { Plus, Target } from 'lucide-react'
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
import { useBodyLogs, useWorkouts } from './health/data'
import { DaySummary, GoalModal, MealCards } from './health/DietTab'
import s from './health/Health.module.css'
import { BodyModal, BodyTab, WorkoutModal, WorkoutTab } from './health/OtherSections'
import { StatsTab } from './health/StatsTab'

type Tab = 'stats' | 'meal' | 'workout' | 'body'
type Dialog = { kind: 'goal' } | { kind: 'workout'; log?: WorkoutLog } | { kind: 'body'; log?: HealthLog } | null

export default function HealthPage() {
  const today = useToday()
  // 첫 화면은 통계, 식단·운동·체중수면은 탭 (이슈 #131)
  const [tab, setTab] = useUrlState('tab', 'stats')
  const [date, setDate] = useUrlState('date', today)
  const [dialog, setDialog] = useState<Dialog>(null)
  const [params, setParams] = useSearchParams()

  // 끼니 목록은 기록 히트맵과 같은 쿼리를 써서 캐시를 공유한다 (날짜 필터가 없어 넉넉히 받는다)
  const meals = useList(mealRecords, { size: YEAR_PAGE, sort: 'consumedAt,desc' })
  const all = meals.data?.content ?? []
  const dayMeals = withinDateTimes(all, (r) => r.consumedAt, date, date)
  const workouts = useWorkouts()
  const body = useBodyLogs()
  const from = weekStartOf(today)

  // 빠른 기록(?new=meal 등)으로 들어오면 해당 탭·창 (주소에서 바로 계산)
  const newKind = params.get('new')
  const effectiveTab =
    newKind === 'meal' ? 'meal' : newKind === 'workout' ? 'workout' : newKind === 'body' ? 'body' : tab
  const active: Dialog = dialog ?? (newKind === 'workout' || newKind === 'body' ? ({ kind: newKind } as Dialog) : null)
  const closeDialog = () => {
    setDialog(null)
    if (newKind)
      setParams(
        (p) => {
          const n = new URLSearchParams(p)
          n.delete('new')
          n.set('tab', newKind)
          return n
        },
        { replace: true },
      )
  }

  return (
    <>
      <PageHeader
        title="건강"
        tabs={
          <Tabs<Tab>
            inHeader
            label="건강 탭"
            value={effectiveTab as Tab}
            onChange={(k) => {
              if (newKind) closeDialog()
              setTab(k)
            }}
            items={[
              { key: 'stats', label: '통계' },
              { key: 'meal', label: '식단', count: dayMeals.length || undefined },
              {
                key: 'workout',
                label: '운동',
                count:
                  withinDates(workouts.data?.content ?? [], (w) => w.performedAt, from, shiftDate(from, 6)).length ||
                  undefined,
              },
              { key: 'body', label: '체중 · 수면', count: body.data?.totalElements || undefined },
            ]}
          />
        }
      >
        {effectiveTab === 'meal' && <DateNav value={date} onChange={setDate} today={today} />}
        {(effectiveTab === 'meal' || effectiveTab === 'stats') && (
          <Button icon={<Target size={14} />} onClick={() => setDialog({ kind: 'goal' })}>
            식단 목표
          </Button>
        )}
        {effectiveTab === 'workout' && (
          <Button variant="primary" icon={<Plus size={14} />} onClick={() => setDialog({ kind: 'workout' })}>
            운동 추가
          </Button>
        )}
        {effectiveTab === 'body' && (
          <Button variant="primary" icon={<Plus size={14} />} onClick={() => setDialog({ kind: 'body' })}>
            기록 추가
          </Button>
        )}
      </PageHeader>

      <div className={s.page}>
        {effectiveTab === 'stats' && (
          <StatsTab
            records={all}
            loading={meals.isLoading}
            today={today}
            onGoals={() => setDialog({ kind: 'goal' })}
            onOpenDay={(d) => {
              setDate(d)
              setTab('meal')
            }}
          />
        )}
        {effectiveTab === 'meal' && (
          <>
            <DaySummary records={dayMeals} date={date} onGoals={() => setDialog({ kind: 'goal' })} />
            <MealCards
              records={dayMeals}
              date={date}
              today={today}
              loading={meals.isLoading}
              error={meals.error}
              onRetry={() => void meals.refetch()}
            />
          </>
        )}
        {effectiveTab === 'workout' && (
          <WorkoutTab
            onAdd={() => setDialog({ kind: 'workout' })}
            onEdit={(log) => setDialog({ kind: 'workout', log })}
          />
        )}
        {effectiveTab === 'body' && (
          <BodyTab onAdd={() => setDialog({ kind: 'body' })} onEdit={(log) => setDialog({ kind: 'body', log })} />
        )}
      </div>

      {active?.kind === 'goal' && <GoalModal onClose={closeDialog} />}
      {active?.kind === 'workout' && <WorkoutModal today={today} log={active.log} onClose={closeDialog} />}
      {active?.kind === 'body' && <BodyModal today={today} log={active.log} onClose={closeDialog} />}
    </>
  )
}
