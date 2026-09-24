import { Plus } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { mealRecords, useDietGoal, workoutLogs } from '../api/health'
import { memos } from '../api/memo'
import { BIG_PAGE, useList, useListsByParent } from '../api/resource'
import { studyProgresses, studyTopics } from '../api/study'
import { MEAL_TYPE_KO, MEAL_TYPES, type StudyProgress } from '../api/types'
import { useProfile } from '../api/user'
import { PageContent, PageHeader } from '../components/layout/PageHeader'
import { useQuickRecord } from '../components/layout/quickContext'
import {
  Button,
  Card,
  EmptyState,
  HeatLegend,
  IconButton,
  KpiTile,
  ProgressBar,
  QueryState,
  SectionHeader,
  Tag,
  YearHeatmap,
  barVar,
  cx,
  type BarColor,
} from '../components/ui'
import { goalsStore } from '../config/goals'
import { pledgesStore } from '../config/prefs'
import { ACTIVITY_LABEL, useActivity, YEAR_PAGE, type ActivityDomain } from '../hooks/useActivity'
import { useAllHubLinks } from '../hooks/useHub'
import { logOn, useHabitsWithLogs, useToggleHabit } from '../hooks/useLife'
import { useAllEvents } from '../hooks/useEvents'
import { useIsMobile } from '../hooks/useMediaQuery'
import { useOptimistic } from '../hooks/useOptimistic'
import { useSemesterBundle } from '../hooks/usePknu'
import { useToday } from '../hooks/useToday'
import { datePart, formatHeaderDate, formatMinutes, shiftDate, weekDays, weekStartOf } from '../lib/date'
import { initials, num, pct, splitTags } from '../lib/format'
import { totalIn } from '../lib/heatmap'
import { dayTotals, dietRows, goalState, mealsOn, ruleText } from '../lib/select/diet'
import { withinDates } from '../lib/select/range'
import { eventsOn, eventTimeLabel } from '../lib/select/schedule'
import { useStore } from '../lib/storage'
import s from './home/Home.module.css'

export default function HomePage() {
  const today = useToday()
  const quick = useQuickRecord()
  const isMobile = useIsMobile()
  const range = useYearRange()
  return (
    <>
      <PageHeader hideOnMobile>
        <span className="mono muted" style={{ fontSize: 12 }}>
          {formatHeaderDate(today)}
        </span>
        <Button variant="primary" icon={<Plus size={14} />} onClick={quick} title="⌘ + Enter">
          Quick Log
        </Button>
      </PageHeader>
      <PageContent>
        <ProfileRow />
        <Kpis />
        {isMobile ? (
          <>
            <MobileHeat />
            <HabitsCard />
            <EventsCard />
            <DietCard />
          </>
        ) : (
          <>
            <div className={s.row3}>
              <HeatCard range={range} />
              <ActivitySplit range={range} />
            </div>
            <div className={s.row3}>
              <EventsCard />
              <HabitsCard />
              <MemosCard />
            </div>
            <DietCard />
            <HubCard />
          </>
        )}
      </PageContent>
    </>
  )
}

// ---- 프로필 한 줄 ----

function ProfileRow() {
  const profile = useProfile()
  const pknu = useSemesterBundle()
  const pledges = useStore(pledgesStore)
  const credits = pknu.courses.reduce((a, c) => a + c.credit, 0)
  const tones = ['green', 'yellow'] as const
  return (
    <section className={s.profile}>
      <div className={s.avatar} aria-hidden="true">
        {initials(profile.data?.displayName)}
      </div>
      <div className={s.who}>
        <h1>{profile.data?.displayName ?? (profile.isLoading ? '…' : 'No profile')}</h1>
      </div>
      <div className={s.tags}>
        {pknu.semester && (
          <Tag size="lg">
            {pknu.semester.name} · {credits}학점
          </Tag>
        )}
        {pledges.slice(0, 2).map((p, i) => (
          <Tag key={p} size="lg" tone={tones[i]}>
            {p}
          </Tag>
        ))}
      </div>
    </section>
  )
}

// ---- KPI 4칸 ----

/** 이번 주 공부 기록 (주제별 병렬 조회) */
function useWeekStudy() {
  const today = useToday()
  const topics = useList(studyTopics, { size: BIG_PAGE })
  const ids = useMemo(() => (topics.data?.content ?? []).map((t) => t.id), [topics.data])
  const prog = useListsByParent<StudyProgress>(studyProgresses, 'topicId', ids, {
    size: BIG_PAGE,
    sort: 'studiedAt,desc',
  })
  const from = weekStartOf(today)
  const week = withinDates(prog.data, (p) => p.studiedAt, from, shiftDate(from, 6))
  return {
    minutes: week.reduce((a, p) => a + p.minutes, 0),
    sessions: week.length,
    isLoading: topics.isLoading || prog.isLoading,
  }
}

function Kpis() {
  const today = useToday()
  const goals = useStore(goalsStore)
  const goal = useDietGoal()
  const recs = useList(mealRecords, { size: YEAR_PAGE, sort: 'consumedAt,desc' })
  const t = dayTotals(recs.data?.content ?? [], today)
  const study = useWeekStudy()
  const workouts = useList(workoutLogs, { size: BIG_PAGE, sort: 'performedAt,desc' })
  const from = weekStartOf(today)
  const weekWorkouts = withinDates(workouts.data?.content ?? [], (w) => w.performedAt, from, shiftDate(from, 6))
  const workoutMinutes = weekWorkouts.reduce((a, w) => a + w.durationMinutes, 0)
  const dash = (loading: boolean, v: string) => (loading ? '…' : v)
  return (
    <section aria-label="오늘 요약" className={s.kpis}>
      <KpiTile
        label="Today's Intake"
        value={dash(recs.isLoading, num(t.kcal))}
        unit={goal.data?.calories ? `/ ${num(goal.data.calories)} kcal` : 'kcal'}
        progress={goal.data?.calories ? pct(t.kcal, goal.data.calories) : undefined}
        color="blue"
        over={goalState(t.kcal, goal.data?.calories, goal.data?.caloriesRule ?? 'AT_MOST') === 'over'}
      />
      <KpiTile
        label="Protein"
        value={dash(recs.isLoading, num(t.proteinG))}
        unit={goal.data?.proteinG ? `/ ${num(goal.data.proteinG)} g` : 'g'}
        progress={goal.data?.proteinG ? pct(t.proteinG, goal.data.proteinG) : undefined}
        color="green"
      />
      <KpiTile
        label="Study This Week"
        value={dash(study.isLoading, formatMinutes(study.minutes))}
        unit={`${study.sessions} sessions`}
        progress={pct(study.minutes, goals.studyWeekMinutes)}
        color="purple"
      />
      <KpiTile
        label="Workout This Week"
        value={dash(workouts.isLoading, formatMinutes(workoutMinutes))}
        unit={`${weekWorkouts.length} sessions · goal 150 min`}
        progress={pct(workoutMinutes, 150)}
        color="orange"
      />
    </section>
  )
}

// ---- 기록 히트맵 + 활동 개요 ----

const DOMAIN_COLOR: Record<ActivityDomain, BarColor> = {
  meal: 'green',
  study: 'purple',
  habit: 'blue',
  workout: 'yellow',
  body: 'gray',
  event: 'orange',
}

function useYearRange() {
  const today = useToday()
  const thisYear = Number(today.slice(0, 4))
  const [year, setYear] = useState(thisYear)
  // 올해는 "최근 1년"(오늘까지 53주), 지난해는 그해 12월 31일까지
  const end = year === thisYear ? today : `${year}-12-31`
  const from = shiftDate(end, -370)
  return { year, setYear, thisYear, end, from }
}

type YearRange = ReturnType<typeof useYearRange>

function HeatCard({ range }: { range: YearRange }) {
  const activity = useActivity()
  const { year, setYear, thisYear, end, from } = range
  const total = totalIn(activity.counts, from, end)
  return (
    <Card className={s.span2}>
      <SectionHeader
        title={
          <>
            {year === thisYear ? 'Last 12 Months' : year} · <span className="mono">{num(total)}</span> records
          </>
        }
        meta={<span className={s.desktopOnly}>meals · workouts · weight · study · habits · past events, per day</span>}
        actions={
          <div className={s.yearBtns}>
            {[thisYear, thisYear - 1].map((y) => (
              <button key={y} type="button" className={s.yearBtn} aria-pressed={y === year} onClick={() => setYear(y)}>
                {y}
              </button>
            ))}
          </div>
        }
      />
      <QueryState loading={activity.isLoading && activity.counts.size === 0} error={activity.error} lines={4}>
        <YearHeatmap counts={activity.counts} end={end} label={`${year}년 일별 기록 수`} />
      </QueryState>
      <HeatLegend />
    </Card>
  )
}

function MobileHeat() {
  const today = useToday()
  const activity = useActivity()
  const total = totalIn(activity.counts, shiftDate(today, -7 * 17), today)
  return (
    <Card>
      <SectionHeader
        title="Last 17 Weeks"
        actions={
          <span className="mono muted" style={{ fontSize: 11.5 }}>
            {num(total)}건
          </span>
        }
      />
      <QueryState loading={activity.isLoading && activity.counts.size === 0} error={activity.error}>
        <YearHeatmap
          counts={activity.counts}
          end={today}
          weeks={17}
          cell={14}
          labels={false}
          label="최근 17주 일별 기록 수"
        />
      </QueryState>
    </Card>
  )
}

function ActivitySplit({ range }: { range: YearRange }) {
  const activity = useActivity()
  const { end, from } = range
  const rows = (Object.keys(activity.byDomain) as ActivityDomain[])
    .map((k) => ({ k, n: activity.byDomain[k].filter((d) => d >= from && d <= end).length }))
    .filter((r) => r.n > 0)
    .sort((a, b) => b.n - a.n)
  const sum = rows.reduce((a, r) => a + r.n, 0)
  return (
    <Card>
      <SectionHeader title="Activity Overview" />
      <QueryState
        loading={activity.isLoading && sum === 0}
        error={activity.error}
        empty={sum === 0}
        emptyView={<EmptyState compact title="No records yet" />}
      >
        <div className={s.split} role="img" aria-label="도메인별 기록 비율">
          {rows.map((r) => (
            <span key={r.k} style={{ width: `${(r.n / sum) * 100}%`, background: barVar(DOMAIN_COLOR[r.k]) }} />
          ))}
        </div>
        {rows.map((r) => (
          <div key={r.k} className={s.splitRow}>
            <span className={s.sdot} style={{ background: barVar(DOMAIN_COLOR[r.k]) }} />
            <span>{ACTIVITY_LABEL[r.k]}</span>
            <span>{r.n / sum < 0.005 ? '<1' : Math.round((r.n / sum) * 100)}%</span>
          </div>
        ))}
      </QueryState>
    </Card>
  )
}

// ---- 오늘 일정 ----

function EventsCard() {
  const today = useToday()
  const list = useAllEvents()
  const todays = eventsOn(list.data?.content ?? [], today)
  return (
    <Card>
      <SectionHeader title="Today's Schedule" count={todays.length} actions={<Link to="/schedule">Calendar</Link>} />
      <QueryState
        loading={list.isLoading}
        error={list.error}
        onRetry={() => void list.refetch()}
        empty={todays.length === 0}
        emptyView={<EmptyState compact title="Nothing scheduled today" />}
      >
        {todays.slice(0, 5).map((e) => (
          <div key={e.id} className={s.event}>
            <span className={s.time}>{eventTimeLabel(e, today)}</span>
            <div className={s.stack}>
              <span className={cx(s.title, 'ellipsis')}>{e.title}</span>
              {e.location && <span className={s.sub}>{e.location}</span>}
            </div>
          </div>
        ))}
      </QueryState>
    </Card>
  )
}

// ---- 오늘 습관 ----

function HabitsCard() {
  const today = useToday()
  const life = useHabitsWithLogs()
  const { toggle } = useToggleHabit()
  const opt = useOptimistic<number>()
  const days = weekDays(today)
  const isDone = (id: number) => opt.value(id, !!logOn(life.logsByHabit.get(id), today)?.completed)
  const doneToday = life.habits.filter((h) => isDone(h.id)).length
  return (
    <Card>
      <SectionHeader
        title="Today's Habits"
        count={life.habits.length ? `${doneToday}/${life.habits.length}` : undefined}
        actions={<Link to="/memo?tab=habits">Habits</Link>}
      />
      <QueryState
        loading={life.isLoading}
        error={life.error}
        onRetry={life.refetch}
        empty={life.habits.length === 0}
        emptyView={<EmptyState compact title="No habits yet" action={<Link to="/memo?tab=habits">Add</Link>} />}
      >
        {life.habits.map((h) => {
          const logs = life.logsByHabit.get(h.id)
          const done = isDone(h.id)
          return (
            <label key={h.id} className={s.line} style={{ cursor: 'pointer', padding: '5px 0' }}>
              <input
                type="checkbox"
                className={s.check}
                style={{ accentColor: 'var(--green)' }}
                checked={done}
                onChange={() => {
                  if (toggle(h, logs, today, { onSettled: () => opt.clear(h.id) })) opt.set(h.id, !done)
                }}
              />
              <span className={cx(s.title, 'ellipsis')} style={{ flexGrow: 1 }}>
                {h.name}
              </span>
              <span className={cx(s.weekDots, s.desktopOnly)} aria-label="이번 주 기록">
                {days.map((d) => (
                  <span key={d} data-on={!!logOn(logs, d)?.completed} title={d} />
                ))}
              </span>
            </label>
          )
        })}
      </QueryState>
    </Card>
  )
}

// ---- 오늘 식단 ----

function DietCard() {
  const today = useToday()
  const goal = useDietGoal()
  const recs = useList(mealRecords, { size: YEAR_PAGE, sort: 'consumedAt,desc' })
  const all = recs.data?.content ?? []
  const meals = mealsOn(all, today)
  const rows = dietRows(goal.data, dayTotals(all, today))
  const hasGoal = rows.some((r) => r.goal !== null)
  return (
    <Card>
      <SectionHeader
        title="Today's Meals"
        meta={!hasGoal && <span className={s.desktopOnly}>Set diet goals in Health</span>}
        actions={<Link to="/health?tab=meal">Health</Link>}
      />
      <QueryState loading={recs.isLoading} error={recs.error} onRetry={() => void recs.refetch()} lines={3}>
        <div className={s.macros}>
          {rows.map((r) => {
            const st = goalState(r.value, r.goal, r.rule)
            return (
              <div key={r.key} className={s.macro}>
                <div className={s.macroHead}>
                  <span>{r.label}</span>
                  <span>
                    {num(r.value, r.key === 'calories' ? 0 : 1)}
                    {r.unit === 'g' ? 'g' : ''}
                  </span>
                </div>
                {r.goal !== null ? (
                  <ProgressBar value={pct(r.value, r.goal)} color={r.color} over={st === 'over'} label={r.label} />
                ) : (
                  <div className={s.noGoalTrack} />
                )}
                <span className={s.limit}>{ruleText(r.goal, r.rule, r.unit)}</span>
              </div>
            )
          })}
        </div>
        <div className={s.meals}>
          {MEAL_TYPES.map((type) => {
            const m = meals[type]
            return (
              <Link key={type} to="/health?tab=meal" className={s.meal}>
                <div className={s.mealHead}>
                  <span>{MEAL_TYPE_KO[type]}</span>
                  <span>{m ? num(m.kcal) : '—'}</span>
                </div>
                <span className={cx(s.sub, 'ellipsis')}>{m ? m.title || 'Untitled' : 'Not logged'}</span>
              </Link>
            )
          })}
        </div>
      </QueryState>
    </Card>
  )
}

// ---- 허브 ----

function abbr(name: string): string {
  const latin = name.replace(/[^A-Za-z0-9]/g, '')
  return (latin.length >= 2 ? latin.slice(0, 2) : name.slice(0, 2)).toUpperCase()
}

function HubCard() {
  const hub = useAllHubLinks()
  const navigate = useNavigate()
  return (
    <Card className={s.span2}>
      <SectionHeader
        title="HUB"
        count={`${hub.categories.length} categories · ${hub.links.length} links`}
        actions={
          <IconButton
            label="Add link"
            size="sm"
            style={{ border: '1px solid var(--border)' }}
            onClick={() => navigate('/study')}
          >
            <Plus size={14} />
          </IconButton>
        }
      />
      <QueryState
        loading={hub.isLoading}
        error={hub.error}
        onRetry={hub.refetch}
        empty={hub.categories.length === 0}
        emptyView={<EmptyState compact title="No categories" action={<Link to="/study">Study</Link>} />}
      >
        <div className={s.hubGrid}>
          {hub.categories.slice(0, 9).map((c) => (
            <Link key={c.id} to={`/study?hub=${c.id}`} className={s.hubItem}>
              <span className={s.abbr}>{abbr(c.name)}</span>
              <span className="ellipsis" style={{ flexGrow: 1 }}>
                {c.name}
              </span>
              <span className="mono muted" style={{ fontSize: 11 }}>
                {hub.linksByCategory.get(c.id)?.length ?? 0}
              </span>
            </Link>
          ))}
        </div>
      </QueryState>
    </Card>
  )
}

// ---- 메모 ----

function MemosCard() {
  const list = useList(memos, { size: 4, sort: 'updatedAt,desc' })
  const items = list.data?.content ?? []
  return (
    <Card>
      <SectionHeader title="Memo" actions={<Link to="/memo">All</Link>} />
      <QueryState
        loading={list.isLoading}
        error={list.error}
        onRetry={() => void list.refetch()}
        empty={items.length === 0}
        emptyView={<EmptyState compact title="No memos" action={<Link to="/memo?new=1">Write</Link>} />}
      >
        {items.map((m) => (
          <Link key={m.id} to={`/memo?id=${m.id}`} className={s.memo}>
            <span className="ellipsis">{m.title}</span>
            {splitTags(m.tags).length > 0 && (
              <span className={s.memoTags}>
                {splitTags(m.tags)
                  .slice(0, 3)
                  .map((t) => (
                    <span key={t}>{t}</span>
                  ))}
              </span>
            )}
            {splitTags(m.tags).length === 0 && <span className={s.sub}>{datePart(m.updatedAt)}</span>}
          </Link>
        ))}
      </QueryState>
    </Card>
  )
}
