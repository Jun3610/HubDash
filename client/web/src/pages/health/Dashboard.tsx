import { Target } from 'lucide-react'
import { useDietGoal } from '../../api/health'
import type { MealRecord } from '../../api/types'
import {
  BarChart,
  Button,
  Card,
  EmptyState,
  LineChart,
  ProgressBar,
  QueryState,
  SectionHeader,
  StackedBarChart,
  barVar,
  openable,
  openClass,
} from '../../components/ui'
import { datePart, formatShortDate, shiftDate, weekStartOf, type LocalDate } from '../../lib/date'
import { num, pct } from '../../lib/format'
import { dailySeries, goalState, MACROS, ruleText } from '../../lib/select/diet'
import { sortBy, withinDates } from '../../lib/select/range'
import { SLEEP_GOAL, useBodyLogs, useMealStats, useWorkouts } from './data'
import { DaySummary } from './DietTab'
import s from './Health.module.css'

type OpenKind = 'meal' | 'workout' | 'body'

/** 건강 대시보드: 식단·체중·수면·운동을 한눈에 (이슈 #131). 자세한 기록은 작은 창으로 연다 (이슈 #148) */
export function HealthDashboard({
  records,
  loading,
  today,
  onGoals,
  onOpen,
}: {
  records: MealRecord[]
  loading: boolean
  today: LocalDate
  onGoals: () => void
  onOpen: (kind: OpenKind, date?: LocalDate) => void
}) {
  const todayMeals = records.filter((r) => datePart(r.consumedAt) === today)
  return (
    <div className={s.statsGrid}>
      <div {...openable('오늘 식단 열기', () => onOpen('meal'))} className={`${s.spanAll} ${openClass}`}>
        <DaySummary records={todayMeals} date={today} onGoals={onGoals} />
      </div>
      <DietTrend records={records} loading={loading} today={today} onGoals={onGoals} onOpen={onOpen} />
      <MacroAverages records={records} today={today} onGoals={onGoals} onOpen={() => onOpen('meal')} />
      <WeightTrend today={today} onOpen={() => onOpen('body')} />
      <SleepTrend today={today} onOpen={() => onOpen('body')} />
      <WeeklyAverages onOpen={() => onOpen('meal')} />
      <WorkoutWeek today={today} onOpen={() => onOpen('workout')} />
    </div>
  )
}

function Legend() {
  return (
    <span className={s.legend}>
      {MACROS.map((m) => (
        <span key={m.key}>
          <i style={{ background: barVar(m.color) }} />
          {m.label}
        </span>
      ))}
    </span>
  )
}

function DietTrend({
  records,
  loading,
  today,
  onGoals,
  onOpen,
}: {
  records: MealRecord[]
  loading: boolean
  today: LocalDate
  onGoals: () => void
  onOpen: (kind: 'meal', date?: LocalDate) => void
}) {
  const goal = useDietGoal()
  const days = dailySeries(records, today, 14)
  const logged = days.filter((d) => d.meals > 0)
  const avg = logged.length ? Math.round(logged.reduce((a, d) => a + d.kcal, 0) / logged.length) : 0
  return (
    <Card {...openable('식단 기록 열기', () => onOpen('meal'))} className={`${s.span2} ${openClass}`}>
      <SectionHeader
        title="Diet · Last 14 Days"
        meta={logged.length ? `avg ${num(avg)} kcal / logged day` : undefined}
        actions={<Legend />}
      />
      <QueryState
        loading={loading}
        error={null}
        empty={logged.length === 0}
        emptyView={<EmptyState compact title="최근 14일 식단 기록이 없어요 — 식단 추가에서 넣어 보세요" />}
      >
        <StackedBarChart
          label="최근 14일 날짜별 칼로리(지방·탄수·단백질)"
          goal={goal.data?.calories ?? null}
          goalLabel={goal.data?.calories ? `권장 ${num(goal.data.calories)}` : undefined}
          data={days.map((d) => ({
            key: d.date,
            title: `${d.date} · ${num(d.kcal)} kcal (지 ${num(d.fatG)}g · 탄 ${num(d.carbsG)}g · 단 ${num(d.proteinG)}g)`,
            parts: MACROS.map((m) => ({ label: m.label, color: m.color, value: d[m.key] * m.kcalPerG })),
          }))}
          axis={[formatShortDate(days[0].date), formatShortDate(days[days.length - 1].date)]}
        />
        <div className={s.dayChips}>
          {days.slice(-7).map((d) => (
            <button key={d.date} type="button" onClick={() => onOpen('meal', d.date)} title="그날 식단 보기">
              {formatShortDate(d.date)}
              <b>{d.meals ? num(d.kcal) : '—'}</b>
            </button>
          ))}
        </div>
      </QueryState>
      {!goal.data?.calories && (
        <Button
          size="sm"
          variant="link"
          icon={<Target size={13} />}
          onClick={onGoals}
          style={{ alignSelf: 'flex-start' }}
        >
          권장 칼로리를 정하면 그래프에 기준선이 보여요
        </Button>
      )}
    </Card>
  )
}

function MacroAverages({
  records,
  today,
  onGoals,
  onOpen,
}: {
  records: MealRecord[]
  today: LocalDate
  onGoals: () => void
  onOpen: () => void
}) {
  const goal = useDietGoal()
  const days = dailySeries(records, today, 7).filter((d) => d.meals > 0)
  const n = days.length || 1
  const avg = {
    carbsG: days.reduce((a, d) => a + d.carbsG, 0) / n,
    fatG: days.reduce((a, d) => a + d.fatG, 0) / n,
    proteinG: days.reduce((a, d) => a + d.proteinG, 0) / n,
  }
  const g = goal.data
  const rows = [
    { label: '지방', value: avg.fatG, goal: g?.fatG ?? null, rule: g?.fatRule ?? 'AT_MOST', color: 'orange' as const },
    {
      label: '탄수',
      value: avg.carbsG,
      goal: g?.carbsG ?? null,
      rule: g?.carbsRule ?? 'AT_MOST',
      color: 'yellow' as const,
    },
    {
      label: '단백질',
      value: avg.proteinG,
      goal: g?.proteinG ?? null,
      rule: g?.proteinRule ?? 'AT_LEAST',
      color: 'green' as const,
    },
  ]
  return (
    <Card {...openable('식단 기록 열기', onOpen)}>
      <SectionHeader
        title="7-Day Daily Average"
        meta={`${days.length} days logged`}
        actions={
          <Button size="sm" variant="link" onClick={onGoals}>
            목표
          </Button>
        }
      />
      {days.length === 0 ? (
        <EmptyState compact title="최근 7일 기록이 없어요" />
      ) : (
        rows.map((r) => {
          const st = goalState(r.value, r.goal, r.rule)
          return (
            <div key={r.label} className={s.avgRow}>
              <div className={s.macroHead}>
                <span>{r.label}</span>
                <span>{num(r.value, 1)}g</span>
              </div>
              {r.goal !== null ? (
                <ProgressBar value={pct(r.value, r.goal)} color={r.color} over={st === 'over'} label={r.label} />
              ) : (
                <div className={s.noGoalTrack} />
              )}
              <span
                className={s.limit}
                style={{ color: st === 'ok' ? 'var(--green)' : st === 'over' ? 'var(--red)' : undefined }}
              >
                {ruleText(r.goal, r.rule, 'g')}
              </span>
            </div>
          )
        })
      )}
    </Card>
  )
}

/** 날짜별 마지막 값 (하루에 여러 번 쟀으면 가장 늦은 기록) */
function lastPerDay<T extends { recordedAt: string }>(logs: T[], pick: (l: T) => number | null, days: LocalDate[]) {
  const sorted = sortBy(logs, (l) => l.recordedAt)
  const by = new Map<LocalDate, number>()
  for (const l of sorted) {
    const v = pick(l)
    if (v !== null) by.set(datePart(l.recordedAt), v)
  }
  return days.map((d) => ({ key: d, value: by.get(d) ?? null }))
}

function WeightTrend({ today, onOpen }: { today: LocalDate; onOpen: () => void }) {
  const list = useBodyLogs()
  const days = Array.from({ length: 30 }, (_, i) => shiftDate(today, i - 29))
  const points = lastPerDay(list.data?.content ?? [], (l) => l.weightKg, days)
  const vals = points.filter((p) => p.value !== null)
  const last = vals[vals.length - 1]?.value ?? null
  const first = vals[0]?.value ?? null
  const delta = last !== null && first !== null && vals.length > 1 ? last - first : null
  return (
    <Card {...openable('체중 · 수면 기록 열기', onOpen)} className={`${s.span2} ${openClass}`}>
      <SectionHeader
        title="Weight Trend"
        meta="30 days"
        actions={
          <>
            {last !== null && (
              <span className="mono" style={{ fontSize: 12.5 }}>
                {num(last, 1)}kg
                {delta !== null && (
                  <span style={{ marginLeft: 6, color: delta <= 0 ? 'var(--green)' : 'var(--orange)' }}>
                    {delta > 0 ? '+' : delta < 0 ? '−' : '±'}
                    {Math.abs(delta).toFixed(1)}
                  </span>
                )}
              </span>
            )}
          </>
        }
      />
      <QueryState loading={list.isLoading} error={list.error} onRetry={() => void list.refetch()}>
        <LineChart
          label="최근 30일 체중"
          points={points.map((p) => ({ ...p, title: p.value !== null ? `${p.key} · ${p.value}kg` : undefined }))}
          unit="kg"
          color="green"
          axis={[formatShortDate(days[0]), formatShortDate(days[days.length - 1])]}
        />
      </QueryState>
    </Card>
  )
}

function SleepTrend({ today, onOpen }: { today: LocalDate; onOpen: () => void }) {
  const list = useBodyLogs()
  const days = Array.from({ length: 14 }, (_, i) => shiftDate(today, i - 13))
  const points = lastPerDay(list.data?.content ?? [], (l) => l.sleepHours, days)
  const vals = points.map((p) => p.value).filter((v): v is number => v !== null)
  const avg = vals.length ? vals.reduce((a, b) => a + b, 0) / vals.length : null
  return (
    <Card {...openable('체중 · 수면 기록 열기', onOpen)}>
      <SectionHeader
        title="Sleep"
        meta="14 days"
        actions={
          <>
            {avg !== null && (
              <span className="mono" style={{ fontSize: 12.5 }}>
                평균 {num(avg, 1)}h
              </span>
            )}
          </>
        }
      />
      <QueryState
        loading={list.isLoading}
        error={null}
        empty={vals.length === 0}
        emptyView={<EmptyState compact title="수면 기록이 없어요" />}
      >
        <BarChart
          label="최근 14일 수면 시간"
          height={100}
          goal={SLEEP_GOAL}
          goalLabel={`${SLEEP_GOAL}h`}
          data={points.map((p) => ({
            key: p.key,
            value: p.value ?? 0,
            label: p.value !== null ? `${p.key} · ${p.value}시간` : `${p.key} · 기록 없음`,
            color: p.value !== null && p.value < SLEEP_GOAL ? 'orange' : 'purple',
          }))}
          axis={[formatShortDate(days[0]), formatShortDate(days[days.length - 1])]}
        />
      </QueryState>
    </Card>
  )
}

function WeeklyAverages({ onOpen }: { onOpen: () => void }) {
  const goal = useDietGoal()
  const stats = useMealStats()
  const weeks = sortBy(stats.data?.content ?? [], (w) => w.weekStart)
  return (
    <Card {...openable('식단 기록 열기', onOpen)} className={`${s.span2} ${openClass}`}>
      <SectionHeader title="Weekly Avg Calories" meta="8 weeks" />
      <QueryState
        loading={stats.isLoading}
        error={stats.error}
        empty={weeks.length === 0}
        emptyView={<EmptyState compact title="주간 통계가 아직 없어요 (매주 자동 집계, 설정에서 다시 계산 가능)" />}
      >
        <BarChart
          label="최근 8주 주간 평균 칼로리"
          goal={goal.data?.calories ?? undefined}
          data={weeks.map((w) => ({
            key: w.weekStart,
            value: w.avgCalories ?? 0,
            label: `${w.weekStart} 주 · ${num(w.avgCalories)} kcal · ${w.dayCount}일 기록`,
            color: goal.data?.calories && (w.avgCalories ?? 0) > goal.data.calories ? 'orange' : 'blue',
          }))}
          axis={[formatShortDate(weeks[0]?.weekStart ?? ''), formatShortDate(weeks[weeks.length - 1]?.weekStart ?? '')]}
        />
      </QueryState>
    </Card>
  )
}

function WorkoutWeek({ today, onOpen }: { today: LocalDate; onOpen: () => void }) {
  const list = useWorkouts()
  const items = list.data?.content ?? []
  const from = weekStartOf(today)
  const week = withinDates(items, (w) => w.performedAt, from, shiftDate(from, 6))
  const minutes = week.reduce((a, w) => a + w.durationMinutes, 0)
  return (
    <Card {...openable('운동 기록 열기', onOpen)}>
      <SectionHeader title="This Week's Workout" meta={`${week.length} sessions · ${num(minutes)} min`} />
      <QueryState
        loading={list.isLoading}
        error={list.error}
        empty={items.length === 0}
        emptyView={<EmptyState compact title="운동 기록이 없어요" />}
      >
        <ProgressBar value={pct(minutes, 150)} color="orange" label="권장 150분 대비" thin />
        <span className={s.limit}>권장 주 150분</span>
        {items.slice(0, 4).map((w) => (
          <div key={w.id} className={s.wrow} style={{ gridTemplateColumns: '44px minmax(0, 1fr) auto' }}>
            <span className={s.mono}>{formatShortDate(w.performedAt)}</span>
            <span className="ellipsis" style={{ color: 'var(--text-strong)' }}>
              {w.type}
            </span>
            <span className={s.mono}>{w.durationMinutes}분</span>
          </div>
        ))}
      </QueryState>
    </Card>
  )
}
