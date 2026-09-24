import { Plus } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { healthLogs, workoutLogs } from '../../api/health'
import { useCreate, useRemove, useUpdate } from '../../api/resource'
import type { HealthLog, WorkoutLog } from '../../api/types'
import {
  Button,
  ConfirmDialog,
  EmptyState,
  Field,
  FormError,
  Input,
  Modal,
  QueryState,
  RowActions,
  Table,
  Textarea,
  TimeField,
} from '../../components/ui'
import { toLocalDateTime, type LocalDate } from '../../lib/date'
import { num } from '../../lib/format'
import { hasErrors, maxLen, numRange, optNum, optStr, required, type Errors } from '../../lib/validate'
import { SLEEP_GOAL, useBodyLogs, useWorkouts } from './data'
import s from './Health.module.css'

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
                  <td className="mono">{l.recordedAt.slice(0, 16).replace('T', ' ')}</td>
                  <td className="num mono">{num(l.weightKg, 1)}</td>
                  <td
                    className="num mono"
                    style={{ color: l.sleepHours !== null && l.sleepHours < SLEEP_GOAL ? 'var(--orange)' : undefined }}
                  >
                    {num(l.sleepHours, 1)}
                  </td>
                  <td className={`${s.hideMobile} muted`}>{l.notes}</td>
                  <td style={{ textAlign: 'right' }}>
                    <RowActions
                      label={`${l.recordedAt.slice(0, 16).replace('T', ' ')} 기록`}
                      onEdit={() => onEdit(l)}
                      onDelete={() => setDel(l)}
                    />
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
        message={del && `${del.recordedAt.slice(0, 16).replace('T', ' ')} 기록을 지울까요?`}
        busy={remove.isPending}
        onClose={() => setDel(null)}
        onConfirm={() => del && remove.mutate(del.id, { onSuccess: () => setDel(null) })}
      />
    </section>
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
    recordedAt: log?.recordedAt.slice(0, 10) ?? today,
    time: log ? log.recordedAt.slice(11, 16) : toLocalDateTime(new Date()).slice(11, 16),
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
      time: required(d.time, '시각'),
      weightKg: numRange(d.weightKg, 1, 500),
      sleepHours: numRange(d.sleepHours, 0, 24),
      notes: maxLen(d.notes, 500),
      both: !d.weightKg.trim() && !d.sleepHours.trim() ? '체중이나 수면 중 하나는 입력하세요' : undefined,
    }
    setErrors(errs)
    if (hasErrors(errs)) return
    const body = {
      recordedAt: `${d.recordedAt}T${d.time}:00`,
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
        <Field label="날짜" required error={errors.recordedAt}>
          <Input type="date" mono value={d.recordedAt} onChange={set('recordedAt')} />
        </Field>
        <Field label="시각" required error={errors.time}>
          <TimeField value={d.time} onChange={(v) => setD({ ...d, time: v })} />
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
