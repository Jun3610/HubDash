import { MoreHorizontal, Plus, X } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { mealItems, mealRecords, useDailySummary } from '../../api/health'
import { useCreate, useRemove, useUpdate } from '../../api/resource'
import { MEAL_TYPE_KO, MEAL_TYPES, type MealItem, type MealRecord, type MealType } from '../../api/types'
import {
  Button,
  ConfirmDialog,
  Field,
  FormError,
  IconButton,
  Input,
  Modal,
  ProgressBar,
  QueryState,
  RowActions,
  Select,
  Table,
  Textarea,
  type BarColor,
} from '../../components/ui'
import { goalsStore } from '../../config/goals'
import { formatTime, type LocalDate } from '../../lib/date'
import { num, pct } from '../../lib/format'
import { useStore } from '../../lib/storage'
import { hasErrors, maxLen, numRange, optNum, optStr, required, type Errors } from '../../lib/validate'
import { defaultMealTime } from './data'
import s from './Health.module.css'

export function DailySummaryCard({ date }: { date: LocalDate }) {
  const goals = useStore(goalsStore)
  const summary = useDailySummary(date)
  const t = summary.data?.totals
  const left = t ? goals.calories - t.calories : 0
  const macros: { label: string; val: string; limit: string; v: number; g: number; color: BarColor; over?: boolean }[] =
    t
      ? [
          {
            label: '단백질',
            val: `${num(t.proteinG)}g`,
            limit: `${num(goals.proteinG)} 이상`,
            v: t.proteinG,
            g: goals.proteinG,
            color: 'green',
          },
          {
            label: '탄수화물',
            val: `${num(t.carbsG)}g`,
            limit: `${num(goals.carbsG)} 이하`,
            v: t.carbsG,
            g: goals.carbsG,
            color: 'yellow',
            over: t.carbsG > goals.carbsG,
          },
          {
            label: '지방',
            val: `${num(t.fatG)}g`,
            limit: `${num(goals.fatG)} 이하`,
            v: t.fatG,
            g: goals.fatG,
            color: 'orange',
            over: t.fatG > goals.fatG,
          },
          {
            label: '나트륨',
            val: `${num(t.sodiumMg)}mg`,
            limit: `${num(goals.sodiumMg)} 대 유지`,
            v: t.sodiumMg,
            g: goals.sodiumMg,
            color: 'red',
          },
        ]
      : []
  return (
    <section className={s.summary} aria-label="하루 합계">
      <QueryState loading={summary.isLoading} error={summary.error} onRetry={() => void summary.refetch()} lines={2}>
        <div className={s.total}>
          <span className="muted" style={{ fontSize: 12 }}>
            하루 합계 · daily-summary
          </span>
          <div className={s.totalValue}>
            <span>{num(t?.calories ?? 0)}</span>
            <span>/ {num(goals.calories)} kcal</span>
          </div>
          <span style={{ fontSize: 12, color: left >= 0 ? 'var(--green)' : 'var(--red)' }}>
            {left >= 0 ? `${num(left)} kcal 남음` : `${num(-left)} kcal 초과`}
          </span>
        </div>
        <div className={s.macros}>
          {macros.map((m) => (
            <div key={m.label} className={s.macro}>
              <div className={s.macroHead}>
                <span>{m.label}</span>
                <span>{m.val}</span>
              </div>
              <ProgressBar value={pct(m.v, m.g)} color={m.color} over={m.over} label={m.label} />
              <span className={s.limit}>{m.limit}</span>
            </div>
          ))}
        </div>
      </QueryState>
    </section>
  )
}

/** 끼니 4종 카드. 같은 끼니가 여러 번 기록돼 있으면 한 카드에 합쳐 보여 준다 */
export function MealCards({
  date,
  today,
  records,
  loading,
  error,
  onRetry,
}: {
  date: LocalDate
  today: LocalDate
  records: MealRecord[]
  loading: boolean
  error: unknown
  onRetry: () => void
}) {
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
    <>
      {MEAL_TYPES.map((type) => (
        <MealCard
          key={type}
          type={type}
          date={date}
          today={today}
          records={records.filter((r) => r.mealType === type)}
        />
      ))}
    </>
  )
}

function MealCard({
  type,
  date,
  today,
  records,
}: {
  type: MealType
  date: LocalDate
  today: LocalDate
  records: MealRecord[]
}) {
  const [adding, setAdding] = useState(false)
  const [menu, setMenu] = useState<MealRecord | null>(null)
  const [editItem, setEditItem] = useState<MealItem | null>(null)
  const [deleteItem, setDeleteItem] = useState<MealItem | null>(null)
  const removeItem = useRemove(mealItems)
  const items = records.flatMap((r) => r.items)
  const kcal = records.reduce((a, r) => a + r.totals.calories, 0)
  const times = records.map((r) => formatTime(r.consumedAt)).join(' · ')

  return (
    <article className={s.mealCard} aria-labelledby={`meal-${type}`}>
      <div className={s.mealHead}>
        <h2 id={`meal-${type}`}>{MEAL_TYPE_KO[type]}</h2>
        <span className={s.mealTime}>{times || '—'}</span>
        <span className={s.mealKcal}>{num(kcal)} kcal</span>
        {records.length > 0 && (
          <IconButton label={`${MEAL_TYPE_KO[type]} 메뉴`} size="sm" onClick={() => setMenu(records[0])}>
            <MoreHorizontal size={15} />
          </IconButton>
        )}
      </div>
      {items.length > 0 ? (
        <Table className={s.itemTable}>
          <thead>
            <tr>
              <th scope="col">음식</th>
              <th scope="col" className="num" style={{ width: 64 }}>
                kcal
              </th>
              <th scope="col" className="num" style={{ width: 56 }}>
                탄
              </th>
              <th scope="col" className="num" style={{ width: 56 }}>
                단
              </th>
              <th scope="col" className="num" style={{ width: 56 }}>
                지
              </th>
              <th scope="col" className="num" style={{ width: 72 }}>
                나트륨
              </th>
              <th scope="col" style={{ width: 60 }}>
                <span className="sr-only">동작</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {items.map((it) => (
              <tr key={it.id} className="hover-row">
                <td>{it.name}</td>
                <td className={`num ${s.kcal}`}>{num(it.calories)}</td>
                <td className="num">{num(it.carbsG, 1)}</td>
                <td className="num">{num(it.proteinG, 1)}</td>
                <td className="num">{num(it.fatG, 1)}</td>
                <td className="num">{num(it.sodiumMg)}</td>
                <td style={{ textAlign: 'right' }}>
                  <RowActions label={it.name} onEdit={() => setEditItem(it)} onDelete={() => setDeleteItem(it)} />
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      ) : (
        <div className={s.mealEmpty}>아직 기록이 없어요.</div>
      )}
      {adding ? (
        <ItemForm type={type} date={date} today={today} records={records} onDone={() => setAdding(false)} />
      ) : (
        <button type="button" className={s.addBtn} onClick={() => setAdding(true)}>
          <Plus size={14} />
          음식 추가
        </button>
      )}
      {menu && <MealRecordModal date={date} today={today} record={menu} onClose={() => setMenu(null)} />}
      {editItem && <ItemEditModal item={editItem} onClose={() => setEditItem(null)} />}
      <ConfirmDialog
        open={!!deleteItem}
        title="음식 삭제"
        message={deleteItem && `"${deleteItem.name}"을(를) 지울까요?`}
        busy={removeItem.isPending}
        onClose={() => setDeleteItem(null)}
        onConfirm={() => deleteItem && removeItem.mutate(deleteItem.id, { onSuccess: () => setDeleteItem(null) })}
      />
    </article>
  )
}

// ---- 음식 입력 ----

interface ItemDraft {
  name: string
  calories: string
  carbsG: string
  proteinG: string
  fatG: string
  sodiumMg: string
}
const EMPTY_ITEM: ItemDraft = { name: '', calories: '', carbsG: '', proteinG: '', fatG: '', sodiumMg: '' }

function validateItem(d: ItemDraft): Errors<keyof ItemDraft> {
  return {
    name: required(d.name, '음식 이름') ?? maxLen(d.name, 100),
    calories: required(d.calories, '칼로리') ?? numRange(d.calories, 0, 20000, true),
    carbsG: numRange(d.carbsG, 0, 2000),
    proteinG: numRange(d.proteinG, 0, 2000),
    fatG: numRange(d.fatG, 0, 2000),
    sodiumMg: numRange(d.sodiumMg, 0, 100000),
  }
}

function toItemBody(d: ItemDraft, mealRecordId: number) {
  return {
    mealRecordId,
    name: d.name.trim(),
    calories: Number(d.calories),
    carbsG: optNum(d.carbsG),
    proteinG: optNum(d.proteinG),
    fatG: optNum(d.fatG),
    sodiumMg: optNum(d.sodiumMg),
  }
}

/** 카드 아래 한 줄 입력. 그 끼니 기록이 없으면 끼니를 먼저 만들고 음식을 붙인다 */
function ItemForm({
  type,
  date,
  today,
  records,
  onDone,
}: {
  type: MealType
  date: LocalDate
  today: LocalDate
  records: MealRecord[]
  onDone: () => void
}) {
  const [d, setD] = useState<ItemDraft>(EMPTY_ITEM)
  const [errors, setErrors] = useState<Errors<keyof ItemDraft>>({})
  const createMeal = useCreate(mealRecords)
  const createItem = useCreate(mealItems)
  const busy = createMeal.isPending || createItem.isPending

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const errs = validateItem(d)
    setErrors(errs)
    if (hasErrors(errs)) return
    try {
      const recordId =
        records[records.length - 1]?.id ??
        (
          await createMeal.mutateAsync({
            mealType: type,
            consumedAt: `${date}T${defaultMealTime(type, date, today)}:00`,
            notes: null,
          })
        ).id
      await createItem.mutateAsync(toItemBody(d, recordId))
      setD(EMPTY_ITEM)
      setErrors({})
    } catch {
      // 토스트는 전역에서 띄운다
    }
  }

  const set = (k: keyof ItemDraft) => (e: React.ChangeEvent<HTMLInputElement>) => setD({ ...d, [k]: e.target.value })
  const firstError = Object.values(errors).find(Boolean)
  return (
    <form className={s.itemForm} onSubmit={submit} noValidate aria-label={`${MEAL_TYPE_KO[type]} 음식 추가`}>
      <Input
        autoFocus
        placeholder="음식"
        aria-label="음식 이름"
        value={d.name}
        onChange={set('name')}
        aria-invalid={!!errors.name}
      />
      <Input
        mono
        inputMode="numeric"
        placeholder="kcal"
        aria-label="칼로리"
        value={d.calories}
        onChange={set('calories')}
        aria-invalid={!!errors.calories}
      />
      <Input
        mono
        inputMode="decimal"
        placeholder="탄"
        aria-label="탄수화물(g)"
        value={d.carbsG}
        onChange={set('carbsG')}
        aria-invalid={!!errors.carbsG}
      />
      <Input
        mono
        inputMode="decimal"
        placeholder="단"
        aria-label="단백질(g)"
        value={d.proteinG}
        onChange={set('proteinG')}
        aria-invalid={!!errors.proteinG}
      />
      <Input
        mono
        inputMode="decimal"
        placeholder="지"
        aria-label="지방(g)"
        value={d.fatG}
        onChange={set('fatG')}
        aria-invalid={!!errors.fatG}
      />
      <Input
        mono
        inputMode="numeric"
        placeholder="나트륨"
        aria-label="나트륨(mg)"
        value={d.sodiumMg}
        onChange={set('sodiumMg')}
        aria-invalid={!!errors.sodiumMg}
      />
      <div style={{ display: 'flex', gap: 4 }}>
        <Button type="submit" variant="primary" size="sm" disabled={busy}>
          추가
        </Button>
        <IconButton label="닫기" size="sm" onClick={onDone}>
          <X size={14} />
        </IconButton>
      </div>
      {firstError && (
        <span className={s.formErr} role="alert">
          {firstError}
        </span>
      )}
      {(createMeal.error || createItem.error) && (
        <div className={s.formErr}>
          <FormError error={createMeal.error ?? createItem.error} />
        </div>
      )}
    </form>
  )
}

function ItemEditModal({ item, onClose }: { item: MealItem; onClose: () => void }) {
  const str = (v: number | null) => (v === null || v === undefined ? '' : String(v))
  const [d, setD] = useState<ItemDraft>({
    name: item.name,
    calories: str(item.calories),
    carbsG: str(item.carbsG),
    proteinG: str(item.proteinG),
    fatG: str(item.fatG),
    sodiumMg: str(item.sodiumMg),
  })
  const [errors, setErrors] = useState<Errors<keyof ItemDraft>>({})
  const update = useUpdate(mealItems)
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = validateItem(d)
    setErrors(errs)
    if (hasErrors(errs)) return
    update.mutate({ id: item.id, body: toItemBody(d, item.mealRecordId) }, { onSuccess: onClose })
  }
  const set = (k: keyof ItemDraft) => (e: React.ChangeEvent<HTMLInputElement>) => setD({ ...d, [k]: e.target.value })
  const fields: [keyof ItemDraft, string][] = [
    ['calories', 'kcal'],
    ['carbsG', '탄수화물 (g)'],
    ['proteinG', '단백질 (g)'],
    ['fatG', '지방 (g)'],
    ['sodiumMg', '나트륨 (mg)'],
  ]
  return (
    <Modal
      open
      onClose={onClose}
      title="음식 수정"
      footer={
        <>
          <Button onClick={onClose}>취소</Button>
          <Button variant="primary" type="submit" form="item-edit" disabled={update.isPending}>
            저장
          </Button>
        </>
      }
    >
      <form id="item-edit" onSubmit={submit} noValidate className={s.formGrid}>
        <Field label="음식" required error={errors.name} className={s.full}>
          <Input value={d.name} onChange={set('name')} />
        </Field>
        {fields.map(([k, label]) => (
          <Field key={k} label={label} required={k === 'calories'} error={errors[k]}>
            <Input mono inputMode="decimal" value={d[k]} onChange={set(k)} />
          </Field>
        ))}
        <div className={s.full}>
          <FormError error={update.error} />
        </div>
      </form>
    </Modal>
  )
}

// ---- 끼니 추가 / 수정 ----

export function MealRecordModal({
  date,
  today,
  record,
  onClose,
}: {
  date: LocalDate
  today: LocalDate
  record?: MealRecord
  onClose: () => void
}) {
  const [type, setType] = useState<MealType>(record?.mealType ?? 'LUNCH')
  const [time, setTime] = useState(record ? formatTime(record.consumedAt) : defaultMealTime('LUNCH', date, today))
  const [day, setDay] = useState(record ? record.consumedAt.slice(0, 10) : date)
  const [notes, setNotes] = useState(record?.notes ?? '')
  const [errors, setErrors] = useState<Errors>({})
  const [confirm, setConfirm] = useState(false)
  const create = useCreate(mealRecords)
  const update = useUpdate(mealRecords)
  const remove = useRemove(mealRecords)
  const mutation = record ? update : create

  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = { day: required(day, '날짜'), time: required(time, '시각'), notes: maxLen(notes, 500) }
    setErrors(errs)
    if (hasErrors(errs)) return
    const body = { mealType: type, consumedAt: `${day}T${time}:00`, notes: optStr(notes) }
    if (record) update.mutate({ id: record.id, body }, { onSuccess: onClose })
    else create.mutate(body, { onSuccess: onClose })
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={record ? `${MEAL_TYPE_KO[record.mealType]} 기록` : '끼니 추가'}
      footer={
        <>
          {record && (
            <Button variant="danger" onClick={() => setConfirm(true)} style={{ marginRight: 'auto' }}>
              끼니 삭제
            </Button>
          )}
          <Button onClick={onClose}>취소</Button>
          <Button variant="primary" type="submit" form="meal-form" disabled={mutation.isPending}>
            {record ? '저장' : '추가'}
          </Button>
        </>
      }
    >
      <form id="meal-form" onSubmit={submit} noValidate className={s.formGrid}>
        <Field label="끼니" required className={s.full}>
          <Select
            value={type}
            onChange={(e) => {
              const t = e.target.value as MealType
              setType(t)
              if (!record) setTime(defaultMealTime(t, day, today))
            }}
          >
            {MEAL_TYPES.map((t) => (
              <option key={t} value={t}>
                {MEAL_TYPE_KO[t]}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="날짜" required error={errors.day}>
          <Input type="date" mono value={day} onChange={(e) => setDay(e.target.value)} />
        </Field>
        <Field label="시각" required error={errors.time}>
          <Input type="time" mono value={time} onChange={(e) => setTime(e.target.value)} />
        </Field>
        <Field label="메모" error={errors.notes} className={s.full}>
          <Textarea rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} style={{ minHeight: 52 }} />
        </Field>
        {record && record.items.length > 0 && (
          <span className={`${s.full} muted`} style={{ fontSize: 12 }}>
            음식 {record.items.length}개 · {num(record.totals.calories)} kcal — 끼니를 지우면 음식도 함께 지워져요.
          </span>
        )}
        <div className={s.full}>
          <FormError error={mutation.error} />
        </div>
      </form>
      <ConfirmDialog
        open={confirm}
        title="끼니 삭제"
        message="이 끼니와 안의 음식을 모두 지울까요?"
        busy={remove.isPending}
        onClose={() => setConfirm(false)}
        onConfirm={() => record && remove.mutate(record.id, { onSuccess: onClose })}
      />
    </Modal>
  )
}
