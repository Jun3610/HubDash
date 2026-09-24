import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Pencil, Plus, Target, Trash2 } from 'lucide-react'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { mealItems, mealRecords, useDietGoal, useUpdateDietGoal } from '../../api/health'
import { invalidateDomain } from '../../api/resource'
import { MEAL_TYPE_KO, MEAL_TYPES, type DietGoalRequest, type GoalRule, type MealType } from '../../api/types'
import {
  barVar,
  Button,
  ConfirmDialog,
  Field,
  FormError,
  IconButton,
  Input,
  Modal,
  ProgressBar,
  QueryState,
  Segmented,
  TimeField,
} from '../../components/ui'
import { formatTime, type LocalDate } from '../../lib/date'
import { num, pct } from '../../lib/format'
import {
  dietRows,
  goalState,
  kcalOf,
  MACROS,
  macroShare,
  mealsOn,
  ruleText,
  type MacroKey,
  type MealEntry,
} from '../../lib/select/diet'
import type { MealRecord } from '../../api/types'
import { hasErrors, maxLen, numRange, required, type Errors } from '../../lib/validate'
import { defaultMealTime } from './data'
import s from './Health.module.css'

// ---------------- 목표 표시 ----------------

/** 하루 합계: 지방 → 탄수 → 단백질 → 칼로리, 목표가 있으면 막대로 */
/** 탄단지 칼로리 비율 도넛, 가운데 총 칼로리 (FatSecret처럼, 이슈 #205) */
function MacroDonut({ share, kcal }: { share: Record<MacroKey, number>; kcal: number }) {
  const R = 52
  const C = 2 * Math.PI * R
  // 조각마다 시작 위치를 미리 계산 (렌더 중에 변수를 누적하지 않게)
  const segs = MACROS.map((m, i) => ({
    m,
    len: (share[m.key] / 100) * C,
    start: MACROS.slice(0, i).reduce((a, x) => a + (share[x.key] / 100) * C, 0),
  }))
  return (
    <svg
      width="140"
      height="140"
      viewBox="0 0 140 140"
      role="img"
      aria-label={MACROS.map((m) => `${m.label} ${share[m.key]}%`).join(', ')}
      className={s.donut}
    >
      <circle cx="70" cy="70" r={R} fill="none" stroke="var(--track)" strokeWidth="14" />
      {segs
        .filter(({ m }) => share[m.key] > 0)
        .map(({ m, len, start }) => (
          <circle
            key={m.key}
            cx="70"
            cy="70"
            r={R}
            fill="none"
            stroke={barVar(m.color)}
            strokeWidth="14"
            strokeDasharray={`${Math.max(0, len - 2)} ${C}`}
            strokeDashoffset={-start}
            transform="rotate(-90 70 70)"
          />
        ))}
      <text x="70" y="68" textAnchor="middle" className={s.donutValue}>
        {num(kcal)}
      </text>
      <text x="70" y="86" textAnchor="middle" className={s.donutUnit}>
        kcal
      </text>
    </svg>
  )
}

export function DaySummary({
  records,
  date,
  onGoals,
  column,
}: {
  records: MealRecord[]
  date: LocalDate
  onGoals: () => void
  /** 식단 창 오른쪽 칸: 도넛 + 세로 목록 (이슈 #205) */
  column?: boolean
}) {
  const goal = useDietGoal()
  const meals = Object.values(mealsOn(records, date))
  const t = meals.reduce(
    (a, m) => ({ carbsG: a.carbsG + m.carbsG, fatG: a.fatG + m.fatG, proteinG: a.proteinG + m.proteinG }),
    { carbsG: 0, fatG: 0, proteinG: 0 },
  )
  const kcal = kcalOf(t)
  const rows = dietRows(goal.data, { ...t, kcal })
  const share = macroShare(t)
  const hasAnyGoal = rows.some((r) => r.goal !== null)
  return (
    <section className={column ? `${s.summary} ${s.summaryColumn}` : s.summary} aria-label="하루 합계">
      {column && (
        <div className={s.donutWrap}>
          <MacroDonut share={share} kcal={kcal} />
          <div className={s.donutLegend}>
            {MACROS.map((m) => (
              <span key={m.key}>
                <i style={{ background: barVar(m.color) }} />
                {m.label} {share[m.key]}%
              </span>
            ))}
          </div>
        </div>
      )}
      <div className={s.total} hidden={column}>
        <span className="muted" style={{ fontSize: 12 }}>
          하루 합계 · {meals.length}끼
        </span>
        <div className={s.totalValue}>
          <span>{num(kcal)}</span>
          <span>kcal{goal.data?.calories ? ` / ${num(goal.data.calories)}` : ''}</span>
        </div>
        <div
          className={s.shareBar}
          role="img"
          aria-label={`칼로리 비율 지방 ${share.fatG}% 탄수 ${share.carbsG}% 단백질 ${share.proteinG}%`}
        >
          {MACROS.map((m) => (
            <span key={m.key} style={{ width: `${share[m.key]}%`, background: barVar(m.color) }} />
          ))}
        </div>
        <span className="muted" style={{ fontSize: 11.5 }}>
          {MACROS.map((m) => `${m.label} ${share[m.key]}%`).join(' · ')}
        </span>
      </div>
      {column && (
        // 식단 창 오른쪽: 한 줄짜리 탄단지 (이슈 #215). 색은 탄단지 색만, 넘으면 값만 빨강
        <ul className={s.compactMacros}>
          {rows.map((r) => {
            const st = goalState(r.value, r.goal, r.rule)
            const color = r.key === 'calories' ? 'blue' : r.color
            return (
              <li key={r.key}>
                <div className={s.compactHead}>
                  <i style={{ background: barVar(color) }} />
                  <span className={s.compactLabel}>{r.label}</span>
                  <span className={s.compactValue} data-over={st === 'over'}>
                    {num(r.value, r.key === 'calories' ? 0 : 1)}
                    {r.goal !== null && (
                      <small>
                        {' '}
                        / {r.rule === 'AT_MOST' ? '≤' : '≥'}
                        {num(r.goal)}
                      </small>
                    )}
                    <small> {r.unit}</small>
                  </span>
                </div>
                <div className={s.compactTrack}>
                  <span
                    style={{
                      width: `${r.goal ? Math.min(100, pct(r.value, r.goal)) : 0}%`,
                      background: barVar(st === 'over' ? 'red' : color),
                    }}
                  />
                </div>
              </li>
            )
          })}
        </ul>
      )}
      <div className={s.macros} hidden={column}>
        {rows.map((r) => {
          const st = goalState(r.value, r.goal, r.rule)
          return (
            <div key={r.key} className={s.macro}>
              <div className={s.macroHead}>
                <span>{r.label}</span>
                <span style={{ color: st === 'over' ? 'var(--red)' : undefined }}>
                  {num(r.value, r.key === 'calories' ? 0 : 1)}
                  {r.unit}
                </span>
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
                {r.key === 'calories' && r.goal !== null ? 'goal ' : ''}
                {ruleText(r.goal, r.rule, r.unit)}
                {st === 'ok' && ' ✓'}
              </span>
            </div>
          )
        })}
      </div>
      <Button size="sm" icon={<Target size={13} />} onClick={onGoals} style={{ alignSelf: 'flex-start' }}>
        {hasAnyGoal ? 'Edit Goal' : 'Set Goal'}
      </Button>
    </section>
  )
}

// ---------------- 끼니 카드 ----------------

export function MealCards({
  stacked,
  records,
  date,
  today,
  loading,
  error,
  onRetry,
}: {
  /** 식단 창 왼쪽 칸: 아침·점심·저녁·간식을 한 줄씩 (이슈 #205) */
  stacked?: boolean
  records: MealRecord[]
  date: LocalDate
  today: LocalDate
  loading: boolean
  error: unknown
  onRetry: () => void
}) {
  const [editing, setEditing] = useState<{ type: MealType; entry?: MealEntry } | null>(null)
  const [deleting, setDeleting] = useState<MealEntry | null>(null)
  // 식단 창에서 ⌘/Ctrl + 1~4 → 아침·점심·저녁·간식 입력 창 (이미 있으면 그 끼니 수정, 이슈 #220)
  const mealsRef = useRef<Record<string, MealEntry | undefined>>({})
  useEffect(() => {
    if (!stacked) return
    const onKey = (e: KeyboardEvent) => {
      if (!(e.metaKey || e.ctrlKey) || e.shiftKey || e.altKey) return
      const n = Number(e.key)
      if (!Number.isInteger(n) || n < 1 || n > MEAL_TYPES.length) return
      // 식단 창 위에 다른 창(끼니 입력 등)이 떠 있으면 두지 않는다
      if (document.querySelectorAll('[role="dialog"]').length > 1) return
      e.preventDefault()
      const type = MEAL_TYPES[n - 1]
      setEditing({ type, entry: mealsRef.current[type] })
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [stacked])
  const meals = mealsOn(records, date)
  useEffect(() => {
    mealsRef.current = meals
  })
  const qc = useQueryClient()
  const remove = useMutation({
    mutationFn: async (entry: MealEntry) => {
      for (const r of entry.records) await mealRecords.remove(r.id) // 끼니를 지우면 서버가 항목도 함께 지움
    },
    onSettled: () => invalidateDomain(qc, mealRecords.path),
  })
  if (loading || error) {
    return (
      <div className={s.mealCard} style={{ padding: '12px 14px' }}>
        <QueryState loading={loading} error={error} onRetry={onRetry} lines={4}>
          {null}
        </QueryState>
      </div>
    )
  }
  return (
    <div className={stacked ? `${s.mealGrid} ${s.mealStack}` : s.mealGrid}>
      {MEAL_TYPES.map((type) => {
        const m = meals[type]
        return (
          <article key={type} className={s.mealCard} aria-labelledby={`meal-${type}`}>
            <div className={s.mealHead}>
              <h2 id={`meal-${type}`}>{MEAL_TYPE_KO[type]}</h2>
              <span className={s.mealTime}>{m ? m.records.map((r) => formatTime(r.consumedAt)).join(' · ') : ''}</span>
              <span className={s.mealKcal}>{m ? `${num(m.kcal)} kcal` : ''}</span>
              {m && (
                <>
                  <IconButton
                    label={`${MEAL_TYPE_KO[type]} 수정`}
                    size="sm"
                    onClick={() => setEditing({ type, entry: m })}
                  >
                    <Pencil size={13} />
                  </IconButton>
                  <IconButton label={`${MEAL_TYPE_KO[type]} 삭제`} size="sm" onClick={() => setDeleting(m)}>
                    <Trash2 size={13} />
                  </IconButton>
                </>
              )}
            </div>
            {m ? (
              <button type="button" className={s.mealBody} onClick={() => setEditing({ type, entry: m })}>
                <span className={s.mealTitle}>{m.title || '제목 없음'}</span>
                <span className={s.mealMacros}>
                  {MACROS.map((x) => (
                    <span key={x.key}>
                      <i style={{ background: barVar(x.color) }} />
                      {x.label} <b>{num(m[x.key], 1)}g</b>
                    </span>
                  ))}
                </span>
              </button>
            ) : (
              <button type="button" className={s.addBtn} onClick={() => setEditing({ type })}>
                <Plus size={14} />
                Add {MEAL_TYPE_KO[type]}
              </button>
            )}
          </article>
        )
      })}
      {editing && (
        <MealModal
          type={editing.type}
          entry={editing.entry}
          date={date}
          today={today}
          onClose={() => setEditing(null)}
        />
      )}
      <ConfirmDialog
        open={!!deleting}
        title="끼니 삭제"
        message={deleting && `${MEAL_TYPE_KO[deleting.type]} "${deleting.title || '제목 없음'}"을(를) 지울까요?`}
        busy={remove.isPending}
        onClose={() => setDeleting(null)}
        onConfirm={() => deleting && remove.mutate(deleting, { onSuccess: () => setDeleting(null) })}
      />
    </div>
  )
}

// ---------------- 끼니 입력 ----------------

interface MealDraft {
  title: string
  carbsG: string
  fatG: string
  proteinG: string
  time: string
}

/**
 * 끼니 하나 = 서버 끼니 기록 1건 + 항목 1건(제목과 탄단지). 칼로리는 계산해서 함께 저장한다.
 * 예전 방식으로 음식을 여러 개 넣은 끼니는 저장할 때 하나로 합친다.
 */
function useSaveMeal() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({
      type,
      date,
      entry,
      d,
    }: {
      type: MealType
      date: LocalDate
      entry?: MealEntry
      d: MealDraft
    }) => {
      const macros = { carbsG: Number(d.carbsG) || 0, fatG: Number(d.fatG) || 0, proteinG: Number(d.proteinG) || 0 }
      const consumedAt = `${date}T${d.time}:00`
      const item = (mealRecordId: number) => ({
        mealRecordId,
        name: d.title.trim(),
        calories: kcalOf(macros),
        ...macros,
        sodiumMg: null,
      })
      if (!entry) {
        const rec = await mealRecords.create({ mealType: type, consumedAt, notes: null })
        await mealItems.create(item(rec.id))
        return
      }
      const [first, ...extraRecords] = entry.records
      if (formatTime(first.consumedAt) !== d.time) {
        await mealRecords.update(first.id, { mealType: type, consumedAt, notes: first.notes })
      }
      const [firstItem, ...extraItems] = first.items
      if (firstItem) await mealItems.update(firstItem.id, item(first.id))
      else await mealItems.create(item(first.id))
      for (const it of extraItems) await mealItems.remove(it.id)
      for (const r of extraRecords) await mealRecords.remove(r.id)
    },
    onSettled: () => invalidateDomain(qc, mealRecords.path),
  })
}

function MealModal({
  type,
  entry,
  date,
  today,
  onClose,
}: {
  type: MealType
  entry?: MealEntry
  date: LocalDate
  today: LocalDate
  onClose: () => void
}) {
  const str = (v: number | undefined) => (v === undefined || v === 0 ? '' : String(Math.round(v * 10) / 10))
  const [d, setD] = useState<MealDraft>({
    title: entry?.title ?? '',
    carbsG: str(entry?.carbsG),
    fatG: str(entry?.fatG),
    proteinG: str(entry?.proteinG),
    time: entry ? formatTime(entry.records[0].consumedAt) : defaultMealTime(type, date, today),
  })
  const [errors, setErrors] = useState<Errors<keyof MealDraft>>({})
  const save = useSaveMeal()
  const merged = entry && (entry.records.length > 1 || entry.records.some((r) => r.items.length > 1))
  const kcal = kcalOf({ carbsG: Number(d.carbsG) || 0, fatG: Number(d.fatG) || 0, proteinG: Number(d.proteinG) || 0 })
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs: Errors<keyof MealDraft> = {
      title: required(d.title, '제목') ?? maxLen(d.title, 100),
      carbsG: numRange(d.carbsG, 0, 2000),
      fatG: numRange(d.fatG, 0, 2000),
      proteinG: numRange(d.proteinG, 0, 2000),
      time: required(d.time, '시각'),
    }
    setErrors(errs)
    if (hasErrors(errs)) return
    save.mutate({ type, date, entry, d }, { onSuccess: onClose })
  }
  const set = (k: keyof MealDraft) => (e: React.ChangeEvent<HTMLInputElement>) => setD({ ...d, [k]: e.target.value })
  return (
    <Modal
      open
      onClose={onClose}
      title={`${entry ? 'Edit' : 'Add'} ${MEAL_TYPE_KO[type]}`}
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button variant="primary" type="submit" form="meal-form" disabled={save.isPending}>
            {save.isPending ? 'Saving…' : 'Save'}
          </Button>
        </>
      }
    >
      <form id="meal-form" onSubmit={submit} noValidate className={s.formGrid}>
        <Field label="What you ate" required error={errors.title} className={s.full}>
          <Input autoFocus placeholder="e.g. chicken rice bowl, 2 eggs" value={d.title} onChange={set('title')} />
        </Field>
        <div className={`${s.full} ${s.macroInputs}`}>
          {MACROS.map((m) => (
            <Field key={m.key} label={`${m.label} (g)`} error={errors[m.key]}>
              <Input mono inputMode="decimal" placeholder="0" value={d[m.key]} onChange={set(m.key)} />
            </Field>
          ))}
          <div className={s.kcalBox} aria-live="polite">
            <span>Calories</span>
            <b>{num(kcal)}</b>
            <small>auto</small>
          </div>
        </div>
        <Field label="Time" required error={errors.time}>
          <TimeField value={d.time} onChange={(v) => setD({ ...d, time: v })} />
        </Field>
        <span className={`${s.full} muted`} style={{ fontSize: 11.5 }}>
          Calories = Fat×9 + Carbs×4 + Protein×4
          {merged && ' · 예전에 음식별로 적은 기록은 저장하면 이 한 줄로 합쳐져요.'}
        </span>
        <div className={s.full}>
          <FormError error={save.error} />
        </div>
      </form>
    </Modal>
  )
}

// ---------------- 식단 목표 ----------------

const GOAL_FIELDS = [
  { key: 'fat', value: 'fatG', rule: 'fatRule', label: 'Fat', unit: 'g' },
  { key: 'carbs', value: 'carbsG', rule: 'carbsRule', label: 'Carbs', unit: 'g' },
  { key: 'protein', value: 'proteinG', rule: 'proteinRule', label: 'Protein', unit: 'g' },
  { key: 'calories', value: 'calories', rule: 'caloriesRule', label: 'Calories', unit: 'kcal' },
] as const

export function GoalModal({ onClose }: { onClose: () => void }) {
  const goal = useDietGoal()
  const update = useUpdateDietGoal()
  const g = goal.data
  const [d, setD] = useState<Record<string, string>>(() =>
    Object.fromEntries(
      GOAL_FIELDS.flatMap((f) => [
        [f.value, g?.[f.value] != null ? String(g[f.value]) : ''],
        [f.rule, g?.[f.rule] ?? (f.key === 'protein' ? 'AT_LEAST' : 'AT_MOST')],
      ]),
    ),
  )
  const [errors, setErrors] = useState<Errors>({})
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs: Errors = Object.fromEntries(
      GOAL_FIELDS.map((f) => [
        f.value,
        numRange(d[f.value], 0, f.key === 'calories' ? 20000 : 2000, f.key === 'calories'),
      ]),
    )
    setErrors(errs)
    if (hasErrors(errs)) return
    const val = (k: string) => (d[k].trim() === '' ? null : Number(d[k]))
    const body: DietGoalRequest = {
      carbsG: val('carbsG'),
      carbsRule: d.carbsRule as GoalRule,
      fatG: val('fatG'),
      fatRule: d.fatRule as GoalRule,
      proteinG: val('proteinG'),
      proteinRule: d.proteinRule as GoalRule,
      calories: val('calories'),
      caloriesRule: d.caloriesRule as GoalRule,
    }
    update.mutate(body, { onSuccess: onClose })
  }
  return (
    <Modal
      open
      onClose={onClose}
      title="Diet Goal"
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button variant="primary" type="submit" form="goal-form" disabled={update.isPending || goal.isLoading}>
            Save
          </Button>
        </>
      }
    >
      <form id="goal-form" onSubmit={submit} noValidate style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <span className="muted" style={{ fontSize: 12 }}>
          Leave a value empty to show only the total for it.
        </span>
        {GOAL_FIELDS.map((f) => (
          <div key={f.key} className={s.goalRow}>
            <Field label={`${f.label} (${f.unit})`} error={errors[f.value]}>
              <Input
                mono
                inputMode="decimal"
                placeholder="No goal"
                value={d[f.value]}
                onChange={(e) => setD({ ...d, [f.value]: e.target.value })}
              />
            </Field>
            <Segmented<GoalRule>
              label={`${f.label} rule`}
              value={d[f.rule] as GoalRule}
              onChange={(v) => setD({ ...d, [f.rule]: v })}
              items={[
                { key: 'AT_MOST', label: 'Max' },
                { key: 'AT_LEAST', label: 'Min' },
              ]}
            />
          </div>
        ))}
        <FormError error={update.error} />
      </form>
    </Modal>
  )
}
