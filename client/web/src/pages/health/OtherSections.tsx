import { Plus } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { healthLogs, workoutLogs } from '../../api/health'
import { useCreate, useRemove, useUpdate } from '../../api/resource'
import type { HealthLog, HealthLogWeeklyStat, MealWeeklyStat, WorkoutLog } from '../../api/types'
import {
  BarChart,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  Field,
  FormError,
  Input,
  Modal,
  QueryState,
  RowActions,
  SectionHeader,
  Sparkline,
  Table,
  Textarea,
} from '../../components/ui'
import { goalsStore } from '../../config/goals'
import { formatShortDate, shiftDate, weekStartOf, type LocalDate } from '../../lib/date'
import { num } from '../../lib/format'
import { sortBy, withinDates } from '../../lib/select/range'
import { useStore } from '../../lib/storage'
import { hasErrors, maxLen, numRange, optNum, optStr, required, type Errors } from '../../lib/validate'
import { SLEEP_GOAL, useBodyLogs, useBodyStats, useMealStats, useWorkouts } from './data'
import s from './Health.module.css'

// ================= 오른쪽 칸 =================

export function WeeklyCaloriesCard() {
  const goals = useStore(goalsStore)
  const stats = useMealStats()
  const weeks: MealWeeklyStat[] = sortBy(stats.data?.content ?? [], (w) => w.weekStart)
  const latest = weeks[weeks.length - 1]
  return (
    <Card>
      <SectionHeader
        title="주간 평균 칼로리"
        actions={
          <span className="muted" style={{ fontSize: 11.5 }}>
            최근 8주
          </span>
        }
      />
      <QueryState
        loading={stats.isLoading}
        error={stats.error}
        onRetry={() => void stats.refetch()}
        empty={weeks.length === 0}
        emptyView={
          <EmptyState compact title="주간 통계가 아직 없어요" action={<Link to="/settings">설정에서 계산</Link>} />
        }
      >
        <BarChart
          label="최근 8주 주간 평균 칼로리"
          goal={goals.calories}
          data={weeks.map((w) => ({
            key: w.weekStart,
            value: w.avgCalories ?? 0,
            label: `${w.weekStart} 주 · ${num(w.avgCalories)} kcal`,
            color: (w.avgCalories ?? 0) > goals.calories ? 'orange' : 'blue',
          }))}
          axis={[formatShortDate(weeks[0]?.weekStart ?? ''), formatShortDate(latest?.weekStart ?? '')]}
        />
        {latest && (
          <div className={s.stat3}>
            <div>
              <span>평균 단백질</span>
              <span>{num(latest.avgProteinG)}g</span>
            </div>
            <div>
              <span>평균 탄수</span>
              <span>{num(latest.avgCarbsG)}g</span>
            </div>
            <div>
              <span>기록 일수</span>
              <span>{latest.dayCount}/7</span>
            </div>
          </div>
        )}
      </QueryState>
    </Card>
  )
}

export function WorkoutAsideCard({ today, onAdd }: { today: LocalDate; onAdd: () => void }) {
  const list = useWorkouts()
  const items = list.data?.content ?? []
  const from = weekStartOf(today)
  const week = withinDates(items, (w) => w.performedAt, from, shiftDate(from, 6))
  return (
    <Card>
      <SectionHeader
        title="운동"
        meta={`이번 주 ${week.length}회 · ${num(week.reduce((a, w) => a + w.durationMinutes, 0))}분`}
        actions={
          <Button variant="link" size="sm" onClick={onAdd}>
            + 기록
          </Button>
        }
      />
      <QueryState
        loading={list.isLoading}
        error={list.error}
        onRetry={() => void list.refetch()}
        empty={items.length === 0}
        emptyView={<EmptyState compact title="운동 기록이 없어요" />}
      >
        {items.slice(0, 4).map((w) => (
          <div key={w.id} className={s.wrow} style={{ gridTemplateColumns: '44px minmax(0, 1fr) auto' }}>
            <span className={s.mono}>{formatShortDate(w.performedAt)}</span>
            <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
              <span className="ellipsis" style={{ color: 'var(--text-strong)' }}>
                {w.type}
              </span>
              {w.notes && (
                <span className="ellipsis muted" style={{ fontSize: 11.5 }}>
                  {w.notes}
                </span>
              )}
            </div>
            <span className={s.mono} style={{ color: '#b3b3b3' }}>
              {w.durationMinutes}분
            </span>
          </div>
        ))}
      </QueryState>
    </Card>
  )
}

export function BodyAsideCard({ onAdd }: { onAdd: () => void }) {
  const list = useBodyLogs()
  const logs = sortBy(list.data?.content ?? [], (l) => l.recordedAt) // 오래된 → 최근
  const weights = logs.filter((l) => l.weightKg !== null)
  const lastW = weights[weights.length - 1]
  const prevW = weights[weights.length - 2]
  const delta = lastW && prevW ? (lastW.weightKg as number) - (prevW.weightKg as number) : null
  const recent = logs.slice(-7).filter((l) => l.sleepHours !== null)
  const sleepAvg = recent.length ? recent.reduce((a, l) => a + (l.sleepHours as number), 0) / recent.length : null
  const tail = logs.slice(-14)
  return (
    <Card>
      <SectionHeader
        title="체중 · 수면"
        actions={
          <Button variant="link" size="sm" onClick={onAdd}>
            + 기록
          </Button>
        }
      />
      <QueryState
        loading={list.isLoading}
        error={list.error}
        onRetry={() => void list.refetch()}
        empty={logs.length === 0}
        emptyView={<EmptyState compact title="체중·수면 기록이 없어요" />}
      >
        <div className={s.body2}>
          <div className={s.metric}>
            <span className="muted" style={{ fontSize: 11.5 }}>
              체중
            </span>
            <div className={s.metricValue}>
              <b>{num(lastW?.weightKg, 1)}</b>
              <span>kg</span>
              {delta !== null && (
                <span className="mono" style={{ color: delta <= 0 ? 'var(--green)' : 'var(--orange)' }}>
                  {delta > 0 ? '+' : delta < 0 ? '−' : '±'}
                  {Math.abs(delta).toFixed(1)}
                </span>
              )}
            </div>
            <Sparkline values={tail.map((l) => l.weightKg)} color="green" label="최근 체중 추이" />
          </div>
          <div className={s.metric}>
            <span className="muted" style={{ fontSize: 11.5 }}>
              수면 평균
            </span>
            <div className={s.metricValue}>
              <b>{num(sleepAvg, 1)}</b>
              <span>시간</span>
              <span
                className="mono"
                style={{ color: sleepAvg !== null && sleepAvg < SLEEP_GOAL ? 'var(--orange)' : 'var(--green)' }}
              >
                목표 {SLEEP_GOAL}
              </span>
            </div>
            <Sparkline values={tail.map((l) => l.sleepHours)} color="purple" label="최근 수면 추이" />
          </div>
        </div>
      </QueryState>
    </Card>
  )
}

// ================= 탭 본문 =================

export function WorkoutTab({ onAdd, onEdit }: { onAdd: () => void; onEdit: (w: WorkoutLog) => void }) {
  const list = useWorkouts()
  const remove = useRemove(workoutLogs)
  const [del, setDel] = useState<WorkoutLog | null>(null)
  const items = list.data?.content ?? []
  return (
    <section className={s.listCard}>
      <div className={s.listHead}>
        <h2>운동 기록</h2>
        <span className="mono muted" style={{ fontSize: 11.5 }}>
          {items.length}
        </span>
        <Button variant="primary" size="sm" icon={<Plus size={13} />} onClick={onAdd}>
          운동 추가
        </Button>
      </div>
      <div className={items.length ? undefined : s.pad}>
        <QueryState
          loading={list.isLoading}
          error={list.error}
          onRetry={() => void list.refetch()}
          empty={items.length === 0}
          emptyView={
            <EmptyState
              description="첫 운동을 기록해 보세요."
              action={
                <Button variant="primary" onClick={onAdd}>
                  운동 추가
                </Button>
              }
            />
          }
        >
          <Table>
            <thead>
              <tr>
                <th scope="col">날짜</th>
                <th scope="col">종류</th>
                <th scope="col" className="num">
                  시간
                </th>
                <th scope="col" className="num">
                  소모 kcal
                </th>
                <th scope="col" className={s.hideMobile}>
                  메모
                </th>
                <th scope="col">
                  <span className="sr-only">동작</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {items.map((w) => (
                <tr key={w.id} className="hover-row">
                  <td className="mono">{w.performedAt}</td>
                  <td>{w.type}</td>
                  <td className="num mono">{w.durationMinutes}분</td>
                  <td className="num mono">{num(w.caloriesBurned)}</td>
                  <td className={`${s.hideMobile} muted`}>{w.notes}</td>
                  <td style={{ textAlign: 'right' }}>
                    <RowActions label={w.type} onEdit={() => onEdit(w)} onDelete={() => setDel(w)} />
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </QueryState>
      </div>
      <ConfirmDialog
        open={!!del}
        title="운동 기록 삭제"
        message={del && `${del.performedAt} "${del.type}" 기록을 지울까요?`}
        busy={remove.isPending}
        onClose={() => setDel(null)}
        onConfirm={() => del && remove.mutate(del.id, { onSuccess: () => setDel(null) })}
      />
    </section>
  )
}

export function BodyTab({ onAdd, onEdit }: { onAdd: () => void; onEdit: (l: HealthLog) => void }) {
  const list = useBodyLogs()
  const remove = useRemove(healthLogs)
  const [del, setDel] = useState<HealthLog | null>(null)
  const items = list.data?.content ?? []
  return (
    <section className={s.listCard}>
      <div className={s.listHead}>
        <h2>체중 · 수면 기록</h2>
        <span className="mono muted" style={{ fontSize: 11.5 }}>
          {items.length}
        </span>
        <Button variant="primary" size="sm" icon={<Plus size={13} />} onClick={onAdd}>
          기록 추가
        </Button>
      </div>
      <div className={items.length ? undefined : s.pad}>
        <QueryState
          loading={list.isLoading}
          error={list.error}
          onRetry={() => void list.refetch()}
          empty={items.length === 0}
          emptyView={
            <EmptyState
              description="체중이나 수면 시간을 기록해 보세요."
              action={
                <Button variant="primary" onClick={onAdd}>
                  기록 추가
                </Button>
              }
            />
          }
        >
          <Table>
            <thead>
              <tr>
                <th scope="col">날짜</th>
                <th scope="col" className="num">
                  체중 kg
                </th>
                <th scope="col" className="num">
                  수면 시간
                </th>
                <th scope="col" className={s.hideMobile}>
                  메모
                </th>
                <th scope="col">
                  <span className="sr-only">동작</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {items.map((l) => (
                <tr key={l.id} className="hover-row">
                  <td className="mono">{l.recordedAt}</td>
                  <td className="num mono">{num(l.weightKg, 1)}</td>
                  <td
                    className="num mono"
                    style={{ color: l.sleepHours !== null && l.sleepHours < SLEEP_GOAL ? 'var(--orange)' : undefined }}
                  >
                    {num(l.sleepHours, 1)}
                  </td>
                  <td className={`${s.hideMobile} muted`}>{l.notes}</td>
                  <td style={{ textAlign: 'right' }}>
                    <RowActions label={`${l.recordedAt} 기록`} onEdit={() => onEdit(l)} onDelete={() => setDel(l)} />
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </QueryState>
      </div>
      <ConfirmDialog
        open={!!del}
        title="기록 삭제"
        message={del && `${del.recordedAt} 기록을 지울까요?`}
        busy={remove.isPending}
        onClose={() => setDel(null)}
        onConfirm={() => del && remove.mutate(del.id, { onSuccess: () => setDel(null) })}
      />
    </section>
  )
}

export function StatsTab() {
  const meal = useMealStats()
  const body = useBodyStats()
  const mealRows = meal.data?.content ?? []
  const bodyRows: HealthLogWeeklyStat[] = body.data?.content ?? []
  return (
    <>
      <section className={s.listCard}>
        <div className={s.listHead}>
          <h2>식단 주간 통계</h2>
          <Link to="/settings" style={{ fontSize: 12 }}>
            다시 계산
          </Link>
        </div>
        <div className={mealRows.length ? undefined : s.pad}>
          <QueryState
            loading={meal.isLoading}
            error={meal.error}
            onRetry={() => void meal.refetch()}
            empty={mealRows.length === 0}
          >
            <Table>
              <thead>
                <tr>
                  <th scope="col">주 시작</th>
                  <th scope="col" className="num">
                    기록 일수
                  </th>
                  <th scope="col" className="num">
                    평균 kcal
                  </th>
                  <th scope="col" className="num">
                    탄
                  </th>
                  <th scope="col" className="num">
                    단
                  </th>
                  <th scope="col" className="num">
                    지
                  </th>
                </tr>
              </thead>
              <tbody>
                {mealRows.map((w) => (
                  <tr key={w.id}>
                    <td className="mono">{w.weekStart}</td>
                    <td className="num mono">{w.dayCount}/7</td>
                    <td className="num mono">{num(w.avgCalories)}</td>
                    <td className="num mono">{num(w.avgCarbsG)}</td>
                    <td className="num mono">{num(w.avgProteinG)}</td>
                    <td className="num mono">{num(w.avgFatG)}</td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </QueryState>
        </div>
      </section>
      <section className={s.listCard}>
        <div className={s.listHead}>
          <h2>체중 · 수면 주간 통계</h2>
          <Link to="/settings" style={{ fontSize: 12 }}>
            다시 계산
          </Link>
        </div>
        <div className={bodyRows.length ? undefined : s.pad}>
          <QueryState
            loading={body.isLoading}
            error={body.error}
            onRetry={() => void body.refetch()}
            empty={bodyRows.length === 0}
          >
            <Table>
              <thead>
                <tr>
                  <th scope="col">주 시작</th>
                  <th scope="col" className="num">
                    기록 수
                  </th>
                  <th scope="col" className="num">
                    평균 체중
                  </th>
                  <th scope="col" className="num">
                    평균 수면
                  </th>
                </tr>
              </thead>
              <tbody>
                {bodyRows.map((w) => (
                  <tr key={w.id}>
                    <td className="mono">{w.weekStart}</td>
                    <td className="num mono">{w.logCount}</td>
                    <td className="num mono">{num(w.avgWeightKg, 1)}</td>
                    <td className="num mono">{num(w.avgSleepHours, 1)}</td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </QueryState>
        </div>
      </section>
    </>
  )
}

// ================= 폼 =================

export function WorkoutModal({ today, log, onClose }: { today: LocalDate; log?: WorkoutLog; onClose: () => void }) {
  const [d, setD] = useState({
    performedAt: log?.performedAt ?? today,
    type: log?.type ?? '',
    durationMinutes: log ? String(log.durationMinutes) : '',
    caloriesBurned: log?.caloriesBurned != null ? String(log.caloriesBurned) : '',
    notes: log?.notes ?? '',
  })
  const [errors, setErrors] = useState<Errors<keyof typeof d>>({})
  const create = useCreate(workoutLogs)
  const update = useUpdate(workoutLogs)
  const recentTypes = [...new Set((useWorkouts().data?.content ?? []).map((w) => w.type))].slice(0, 8)
  const m = log ? update : create
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = {
      performedAt: required(d.performedAt, '날짜'),
      type: required(d.type, '종류') ?? maxLen(d.type, 100),
      durationMinutes: required(d.durationMinutes, '시간') ?? numRange(d.durationMinutes, 1, 1440, true),
      caloriesBurned: numRange(d.caloriesBurned, 0, 10000, true),
      notes: maxLen(d.notes, 500),
    }
    setErrors(errs)
    if (hasErrors(errs)) return
    const body = {
      performedAt: d.performedAt,
      type: d.type.trim(),
      durationMinutes: Number(d.durationMinutes),
      caloriesBurned: optNum(d.caloriesBurned),
      notes: optStr(d.notes),
    }
    if (log) update.mutate({ id: log.id, body }, { onSuccess: onClose })
    else create.mutate(body, { onSuccess: onClose })
  }
  const set = (k: keyof typeof d) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setD({ ...d, [k]: e.target.value })
  return (
    <Modal
      open
      onClose={onClose}
      title={log ? '운동 수정' : '운동 기록'}
      footer={
        <>
          <Button onClick={onClose}>취소</Button>
          <Button variant="primary" type="submit" form="workout-form" disabled={m.isPending}>
            저장
          </Button>
        </>
      }
    >
      <form id="workout-form" onSubmit={submit} noValidate className={s.formGrid}>
        <Field label="날짜" required error={errors.performedAt}>
          <Input type="date" mono value={d.performedAt} onChange={set('performedAt')} />
        </Field>
        <Field label="종류" required error={errors.type}>
          <Input list="workout-types" placeholder="웨이트 — 등 + 이두" value={d.type} onChange={set('type')} />
        </Field>
        <datalist id="workout-types">
          {recentTypes.map((t) => (
            <option key={t} value={t} />
          ))}
        </datalist>
        <Field label="시간 (분)" required error={errors.durationMinutes}>
          <Input mono inputMode="numeric" value={d.durationMinutes} onChange={set('durationMinutes')} />
        </Field>
        <Field label="소모 칼로리" error={errors.caloriesBurned}>
          <Input mono inputMode="numeric" value={d.caloriesBurned} onChange={set('caloriesBurned')} />
        </Field>
        <Field label="메모" error={errors.notes} className={s.full}>
          <Textarea rows={2} value={d.notes} onChange={set('notes')} style={{ minHeight: 52 }} />
        </Field>
        <div className={s.full}>
          <FormError error={m.error} />
        </div>
      </form>
    </Modal>
  )
}

export function BodyModal({ today, log, onClose }: { today: LocalDate; log?: HealthLog; onClose: () => void }) {
  const [d, setD] = useState({
    recordedAt: log?.recordedAt ?? today,
    weightKg: log?.weightKg != null ? String(log.weightKg) : '',
    sleepHours: log?.sleepHours != null ? String(log.sleepHours) : '',
    notes: log?.notes ?? '',
  })
  const [errors, setErrors] = useState<Errors<keyof typeof d | 'both'>>({})
  const create = useCreate(healthLogs)
  const update = useUpdate(healthLogs)
  const m = log ? update : create
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = {
      recordedAt: required(d.recordedAt, '날짜'),
      weightKg: numRange(d.weightKg, 1, 500),
      sleepHours: numRange(d.sleepHours, 0, 24),
      notes: maxLen(d.notes, 500),
      both: !d.weightKg.trim() && !d.sleepHours.trim() ? '체중이나 수면 중 하나는 입력하세요' : undefined,
    }
    setErrors(errs)
    if (hasErrors(errs)) return
    const body = {
      recordedAt: d.recordedAt,
      weightKg: optNum(d.weightKg),
      sleepHours: optNum(d.sleepHours),
      notes: optStr(d.notes),
    }
    if (log) update.mutate({ id: log.id, body }, { onSuccess: onClose })
    else create.mutate(body, { onSuccess: onClose })
  }
  const set = (k: keyof typeof d) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setD({ ...d, [k]: e.target.value })
  return (
    <Modal
      open
      onClose={onClose}
      title={log ? '체중 · 수면 수정' : '체중 · 수면 기록'}
      footer={
        <>
          <Button onClick={onClose}>취소</Button>
          <Button variant="primary" type="submit" form="body-form" disabled={m.isPending}>
            저장
          </Button>
        </>
      }
    >
      <form id="body-form" onSubmit={submit} noValidate className={s.formGrid}>
        <Field label="날짜" required error={errors.recordedAt} className={s.full}>
          <Input type="date" mono value={d.recordedAt} onChange={set('recordedAt')} />
        </Field>
        <Field label="체중 (kg)" error={errors.weightKg ?? errors.both}>
          <Input mono inputMode="decimal" value={d.weightKg} onChange={set('weightKg')} />
        </Field>
        <Field label="수면 (시간, 0–24)" error={errors.sleepHours}>
          <Input mono inputMode="decimal" value={d.sleepHours} onChange={set('sleepHours')} />
        </Field>
        <Field label="메모" error={errors.notes} className={s.full}>
          <Textarea rows={2} value={d.notes} onChange={set('notes')} style={{ minHeight: 52 }} />
        </Field>
        <div className={s.full}>
          <FormError error={m.error} />
        </div>
      </form>
    </Modal>
  )
}
