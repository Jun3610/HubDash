import { Bell, BellOff, MoreHorizontal, Plus } from 'lucide-react'
import { useRef, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { memos } from '../api/memo'
import { reminders } from '../api/reminder'
import { BIG_PAGE, useCreate, useList, useRemove, useUpdate } from '../api/resource'
import type { Reminder, ReminderRequest } from '../api/types'
import { useSettings } from '../api/user'
import { PageHeader } from '../components/layout/PageHeader'
import {
  Button,
  Checkbox,
  ConfirmDialog,
  EmptyState,
  Field,
  FormError,
  IconButton,
  Input,
  Modal,
  QueryState,
  Select,
  Tabs,
  Tag,
  type Tone,
} from '../components/ui'
import { useAllEvents } from '../hooks/useEvents'
import { useSemesterBundle } from '../hooks/usePknu'
import { useNow, useToday } from '../hooks/useToday'
import { useUrlState } from '../hooks/useUrlState'
import {
  datePart,
  daysUntil,
  formatShortDate,
  formatTime,
  toLocalDateTime,
  weekdayKo,
  type LocalDateTime,
} from '../lib/date'
import {
  DOMAIN_ROUTE,
  groupReminders,
  presetDayBefore,
  presetOneHour,
  presetTonight,
  snooze,
  untilLabel,
} from '../lib/select/reminder'
import { sortBy } from '../lib/select/range'
import { hasErrors, maxLen, required, type Errors } from '../lib/validate'
import s from './reminders/Reminders.module.css'

type Tab = 'pending' | 'sent' | 'all'

const DOMAIN_TONE: Record<string, Tone> = {
  pknu: 'blue',
  schedule: 'purple',
  health: 'green',
  study: 'yellow',
  life: 'orange',
  memo: 'neutral',
  hub: 'neutral',
}

/** 연결 대상(과제·일정·메모)의 이름과 이동 주소를 찾는다 */
function useTargets() {
  const pknu = useSemesterBundle()
  const ev = useAllEvents()
  const memoList = useList(memos, { size: BIG_PAGE, sort: 'updatedAt,desc' })
  const events = ev.data?.content ?? []
  const courseName = new Map(pknu.courses.map((c) => [c.id, c.name]))
  const resolve = (r: Pick<Reminder, 'targetDomain' | 'targetEntityId'>): { label: string; to: string } | null => {
    const d = r.targetDomain
    if (!d) return null
    const route = DOMAIN_ROUTE[d]
    const id = r.targetEntityId
    if (d === 'pknu' && id) {
      const a = pknu.assignments.find((x) => x.id === id)
      if (a) return { label: `${a.title} · ${courseName.get(a.courseId) ?? ''}`, to: '/pknu' }
    }
    if (d === 'schedule' && id) {
      const e = events.find((x) => x.id === id)
      if (e)
        return {
          label: `${e.title} · ${formatShortDate(datePart(e.startAt))} ${e.allDay ? '종일' : formatTime(e.startAt)}`,
          to: `/schedule?event=${e.id}&date=${datePart(e.startAt)}`,
        }
    }
    if (d === 'memo' && id) {
      const m = memoList.data?.content.find((x) => x.id === id)
      if (m) return { label: m.title, to: `/memo?id=${m.id}` }
    }
    return route ? { label: id ? `${route.label} #${id}` : route.label, to: route.to } : { label: d, to: '/' }
  }
  return { pknu, events, memos: memoList.data?.content ?? [], resolve }
}

export default function RemindersPage() {
  const now = toLocalDateTime(useNow(30_000))
  const [tab, setTab] = useUrlState('tab', 'pending')
  const [editing, setEditing] = useState<Reminder | null>(null)
  const list = useList(reminders, { size: BIG_PAGE, sort: 'targetAt,asc' })
  const all = list.data?.content ?? []
  const g = groupReminders(all, now)
  const targets = useTargets()
  const formRef = useRef<HTMLInputElement>(null)
  const pendingCount = g.overdue.length + g.today.length + g.upcoming.length

  const sections: {
    key: string
    name: string
    hint: string
    dot: string
    items: Reminder[]
    tone?: 'late' | 'soon'
  }[] = []
  if (tab !== 'sent') {
    sections.push(
      {
        key: 'overdue',
        name: '지남 · 아직 안 보냄',
        hint: '알림 시각이 지났지만 sent = false',
        dot: 'var(--red)',
        items: g.overdue,
        tone: 'late',
      },
      {
        key: 'today',
        name: '오늘',
        hint: `${formatShortDate(datePart(now))} ${weekdayKo(datePart(now))}`,
        dot: 'var(--accent)',
        items: g.today,
        tone: 'soon',
      },
      { key: 'upcoming', name: '예정', hint: '내일 이후', dot: 'var(--text-muted)', items: g.upcoming },
    )
  }
  if (tab !== 'pending')
    sections.push({ key: 'sent', name: '보냄', hint: '최근 순', dot: 'var(--green)', items: g.sent })

  return (
    <>
      <PageHeader
        title="리마인더"
        tabs={
          <Tabs<Tab>
            inHeader
            label="리마인더 필터"
            value={tab as Tab}
            onChange={setTab}
            items={[
              { key: 'pending', label: '대기', count: pendingCount },
              { key: 'sent', label: '보냄', count: g.sent.length },
              { key: 'all', label: '전체' },
            ]}
          />
        }
      >
        <Button variant="primary" icon={<Plus size={14} />} onClick={() => formRef.current?.focus()}>
          리마인더 추가
        </Button>
      </PageHeader>

      <div className={s.layout}>
        <div className={s.main}>
          <QueryState loading={list.isLoading} error={list.error} onRetry={() => void list.refetch()} lines={6}>
            {sections.every((x) => x.items.length === 0) ? (
              <div className={s.group}>
                <EmptyState
                  title={tab === 'sent' ? '보낸 리마인더가 없어요' : '대기 중인 리마인더가 없어요'}
                  description="오른쪽에서 새 리마인더를 만들어 보세요."
                />
              </div>
            ) : (
              sections
                .filter((x) => x.items.length > 0 || x.key === 'today')
                .map((sec) => (
                  <section key={sec.key} className={s.group} aria-labelledby={`g-${sec.key}`}>
                    <div className={s.groupHead}>
                      <span className={s.dot} style={{ background: sec.dot }} />
                      <h2 id={`g-${sec.key}`}>{sec.name}</h2>
                      <span className="mono muted" style={{ fontSize: 11.5 }}>
                        {sec.items.length}
                      </span>
                      <span className={s.hint}>{sec.hint}</span>
                    </div>
                    {sec.items.length === 0 ? (
                      <div style={{ padding: '0 14px' }}>
                        <EmptyState compact title="오늘 알림이 없어요" />
                      </div>
                    ) : (
                      sec.items.map((r) => (
                        <ReminderRow
                          key={r.id}
                          reminder={r}
                          now={now}
                          tone={sec.tone}
                          resolve={targets.resolve}
                          onEdit={() => setEditing(r)}
                        />
                      ))
                    )}
                  </section>
                ))
            )}
          </QueryState>
        </div>

        <aside className={s.aside}>
          <NewReminderForm titleRef={formRef} targets={targets} now={now} />
          <NotificationCard />
        </aside>
      </div>

      {editing && <EditModal reminder={editing} onClose={() => setEditing(null)} />}
    </>
  )
}

function ReminderRow({
  reminder: r,
  now,
  tone,
  resolve,
  onEdit,
}: {
  reminder: Reminder
  now: LocalDateTime
  tone?: 'late' | 'soon'
  resolve: ReturnType<typeof useTargets>['resolve']
  onEdit: () => void
}) {
  const today = useToday()
  const update = useUpdate(reminders)
  const remove = useRemove(reminders)
  const [menu, setMenu] = useState(false)
  const [confirm, setConfirm] = useState(false)
  const target = resolve(r)
  const put = (patch: Partial<ReminderRequest>) =>
    update.mutate({
      id: r.id,
      body: {
        title: r.title,
        targetAt: r.targetAt,
        targetDomain: r.targetDomain,
        targetEntityId: r.targetEntityId,
        sent: r.sent,
        ...patch,
      },
    })
  const overdue = !r.sent && r.targetAt < now
  const sub =
    r.sent || overdue || datePart(r.targetAt) !== today
      ? `${formatShortDate(datePart(r.targetAt))} ${weekdayKo(datePart(r.targetAt))}`
      : untilLabel(r.targetAt, now)
  // 디자인: 오늘·예정은 "14:30", 어제 지난 것은 "어제 21:00" — 날짜는 아랫줄에
  const main = daysUntil(datePart(r.targetAt), today) === -1 ? `어제 ${formatTime(r.targetAt)}` : formatTime(r.targetAt)
  return (
    <div className={s.row} data-sent={r.sent}>
      <div className={s.when} data-tone={r.sent ? undefined : tone}>
        <span>{main}</span>
        <span>{sub}</span>
      </div>
      <div className={s.title}>
        <span>{r.title}</span>
        {target && r.targetDomain && (
          <span className={s.target}>
            <Tag tone={DOMAIN_TONE[r.targetDomain] ?? 'neutral'}>{r.targetDomain}</Tag>
            <Link to={target.to}>{target.label}</Link>
          </span>
        )}
      </div>
      {r.sent ? (
        <Button size="sm" disabled={update.isPending} onClick={() => put({ sent: false })}>
          되돌리기
        </Button>
      ) : overdue ? (
        <Button size="sm" disabled={update.isPending} onClick={() => put({ sent: true })}>
          보냄 처리
        </Button>
      ) : (
        <Button
          size="sm"
          disabled={update.isPending}
          onClick={() => put({ targetAt: snooze(r.targetAt, now, '1h') })}
          title="1시간 미루기"
        >
          미루기
        </Button>
      )}
      <IconButton label={`${r.title} 메뉴`} size="sm" aria-expanded={menu} onClick={() => setMenu((v) => !v)}>
        <MoreHorizontal size={15} />
      </IconButton>
      {menu && (
        <>
          <div style={{ position: 'fixed', inset: 0, zIndex: 9 }} onClick={() => setMenu(false)} />
          <div className={s.menu} role="menu">
            {!r.sent && (
              <>
                <button
                  type="button"
                  role="menuitem"
                  onClick={() => {
                    setMenu(false)
                    put({ targetAt: snooze(r.targetAt, now, '1h') })
                  }}
                >
                  1시간 미루기
                </button>
                <button
                  type="button"
                  role="menuitem"
                  onClick={() => {
                    setMenu(false)
                    put({ targetAt: snooze(r.targetAt, now, 'tomorrow') })
                  }}
                >
                  내일 같은 시각
                </button>
                <button
                  type="button"
                  role="menuitem"
                  onClick={() => {
                    setMenu(false)
                    put({ sent: true })
                  }}
                >
                  보냄 처리
                </button>
              </>
            )}
            <button
              type="button"
              role="menuitem"
              onClick={() => {
                setMenu(false)
                onEdit()
              }}
            >
              수정
            </button>
            <button
              type="button"
              role="menuitem"
              className={s.danger}
              onClick={() => {
                setMenu(false)
                setConfirm(true)
              }}
            >
              삭제
            </button>
          </div>
        </>
      )}
      <ConfirmDialog
        open={confirm}
        title="리마인더 삭제"
        message={`"${r.title}"을(를) 지울까요?`}
        busy={remove.isPending}
        onClose={() => setConfirm(false)}
        onConfirm={() => remove.mutate(r.id, { onSuccess: () => setConfirm(false) })}
      />
    </div>
  )
}

// ---- 새 리마인더 ----

const DOMAINS = [
  { key: '', label: '없음' },
  { key: 'pknu', label: 'pknu · 과제' },
  { key: 'schedule', label: 'schedule · 일정' },
  { key: 'memo', label: 'memo · 메모' },
  { key: 'health', label: 'health' },
  { key: 'study', label: 'study' },
  { key: 'life', label: 'life' },
  { key: 'hub', label: 'hub' },
]

/** 목록에서 고를 수 있는 대상이 있는 도메인 */
const HAS_TARGETS = new Set(['pknu', 'schedule', 'memo'])

function NewReminderForm({
  titleRef,
  targets,
  now,
}: {
  titleRef: React.RefObject<HTMLInputElement>
  targets: ReturnType<typeof useTargets>
  now: LocalDateTime
}) {
  const [params, setParams] = useSearchParams()
  // 일정 상세의 "리마인더 만들기"(?new=1&domain=schedule&entity=…)에서 오면 대상을 채워 둔다
  const [d, setD] = useState(() => ({
    title: '',
    at: presetOneHour(now).slice(0, 16),
    domain: params.get('domain') ?? '',
    entity: params.get('entity') ?? '',
  }))
  const [errors, setErrors] = useState<Errors>({})
  const create = useCreate(reminders)

  const options = targetOptions(d.domain, targets, now)
  const entity = HAS_TARGETS.has(d.domain) ? d.entity || options[0]?.id || '' : ''
  const picked = options.find((o) => o.id === entity)

  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = {
      title: required(d.title, '제목') ?? maxLen(d.title, 200),
      at: required(d.at, '알림 시각'),
    }
    setErrors(errs)
    if (hasErrors(errs)) return
    create.mutate(
      {
        title: d.title.trim(),
        targetAt: `${d.at}:00`,
        targetDomain: d.domain || null,
        targetEntityId: entity ? Number(entity) : null,
        sent: false,
      },
      {
        onSuccess: () => {
          setD({ ...d, title: '' })
          if (params.get('new'))
            setParams(
              (p) => {
                const n = new URLSearchParams(p)
                ;['new', 'domain', 'entity'].forEach((k) => n.delete(k))
                return n
              },
              { replace: true },
            )
        },
      },
    )
  }

  return (
    <form aria-label="리마인더 추가" className={s.form} onSubmit={submit} noValidate>
      <h2 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text)' }}>새 리마인더</h2>
      <Field label="제목" required error={errors.title}>
        <Input
          ref={titleRef}
          autoFocus={params.get('new') === '1'}
          placeholder={picked ? picked.suggest : '예: ERD 보고서 초안 끝내기'}
          value={d.title}
          onChange={(e) => setD({ ...d, title: e.target.value })}
        />
      </Field>
      <Field label="알림 시각" required error={errors.at}>
        <Input type="datetime-local" mono value={d.at} onChange={(e) => setD({ ...d, at: e.target.value })} />
      </Field>
      <div className={s.presets} role="group" aria-label="시각 빠른 선택">
        <button type="button" className={s.preset} onClick={() => setD({ ...d, at: presetOneHour(now).slice(0, 16) })}>
          1시간 뒤
        </button>
        <button type="button" className={s.preset} onClick={() => setD({ ...d, at: presetTonight(now).slice(0, 16) })}>
          오늘 밤 9시
        </button>
        <button
          type="button"
          className={s.preset}
          data-accent="true"
          disabled={!picked?.due}
          title={picked?.due ? `${picked.due} 하루 전 21:00` : '마감(시작)이 있는 과제·일정을 고르면 쓸 수 있어요'}
          onClick={() => picked?.due && setD({ ...d, at: presetDayBefore(picked.due).slice(0, 16) })}
        >
          마감 하루 전
        </button>
      </div>
      <div className={s.two}>
        <Field label="연결 도메인">
          <Select value={d.domain} onChange={(e) => setD({ ...d, domain: e.target.value, entity: '' })}>
            {DOMAINS.map((x) => (
              <option key={x.key} value={x.key}>
                {x.label}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="대상">
          <Select
            value={entity}
            onChange={(e) => setD({ ...d, entity: e.target.value })}
            disabled={!HAS_TARGETS.has(d.domain) || options.length === 0}
          >
            {!HAS_TARGETS.has(d.domain) || options.length === 0 ? (
              <option value="">{HAS_TARGETS.has(d.domain) ? '고를 대상이 없어요' : '—'}</option>
            ) : (
              options.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.label}
                </option>
              ))
            )}
          </Select>
        </Field>
      </div>
      <span className="muted" style={{ fontSize: 11.5 }}>
        targetDomain / targetEntityId로 저장돼요. 연결하면 목록에서 바로 이동할 수 있어요.
      </span>
      <FormError error={create.error} />
      <Button type="submit" variant="primary" disabled={create.isPending} style={{ height: 30, fontSize: 13 }}>
        {create.isPending ? '추가 중…' : '추가'}
      </Button>
    </form>
  )
}

/** 도메인별 대상 목록: 과제는 미완료, 일정은 앞으로 있을 것, 메모는 최근 순 */
function targetOptions(domain: string, t: ReturnType<typeof useTargets>, now: LocalDateTime) {
  if (domain === 'pknu')
    return sortBy(
      t.pknu.assignments.filter((a) => !a.completed),
      (a) => a.dueDate,
    ).map((a) => ({ id: String(a.id), label: a.title, due: a.dueDate, suggest: `${a.title} 제출` }))
  if (domain === 'schedule')
    return sortBy(
      t.events.filter((e) => e.endAt >= now),
      (e) => e.startAt,
    )
      .slice(0, 50)
      .map((e) => ({
        id: String(e.id),
        label: `${formatShortDate(datePart(e.startAt))} ${e.title}`,
        due: datePart(e.startAt),
        suggest: e.title,
      }))
  if (domain === 'memo')
    return t.memos
      .slice(0, 50)
      .map((m) => ({ id: String(m.id), label: m.title, due: undefined, suggest: `${m.title} 다시 보기` }))
  return []
}

function NotificationCard() {
  const settings = useSettings()
  const on = !!settings.data?.notificationEnabled
  return (
    <div className={s.noti}>
      {on ? <Bell size={16} color="var(--accent)" /> : <BellOff size={16} color="var(--text-muted)" />}
      <div style={{ display: 'flex', flexDirection: 'column', flexGrow: 1 }}>
        <span style={{ color: 'var(--text)' }}>{settings.isLoading ? '…' : on ? '알림 켜짐' : '알림 꺼짐'}</span>
        <span className="muted" style={{ fontSize: 12 }}>
          설정 › notificationEnabled
        </span>
      </div>
      <Link to="/settings" style={{ fontSize: 12 }}>
        설정
      </Link>
    </div>
  )
}

function EditModal({ reminder: r, onClose }: { reminder: Reminder; onClose: () => void }) {
  const [title, setTitle] = useState(r.title)
  const [at, setAt] = useState(r.targetAt.slice(0, 16))
  const [sent, setSent] = useState(r.sent)
  const [errors, setErrors] = useState<Errors>({})
  const update = useUpdate(reminders)
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = { title: required(title, '제목') ?? maxLen(title, 200), at: required(at, '알림 시각') }
    setErrors(errs)
    if (hasErrors(errs)) return
    update.mutate(
      {
        id: r.id,
        body: {
          title: title.trim(),
          targetAt: `${at}:00`,
          targetDomain: r.targetDomain,
          targetEntityId: r.targetEntityId,
          sent,
        },
      },
      { onSuccess: onClose },
    )
  }
  return (
    <Modal
      open
      onClose={onClose}
      title="리마인더 수정"
      footer={
        <>
          <Button onClick={onClose}>취소</Button>
          <Button variant="primary" type="submit" form="reminder-edit" disabled={update.isPending}>
            저장
          </Button>
        </>
      }
    >
      <form
        id="reminder-edit"
        onSubmit={submit}
        noValidate
        style={{ display: 'flex', flexDirection: 'column', gap: 10 }}
      >
        <Field label="제목" required error={errors.title}>
          <Input value={title} onChange={(e) => setTitle(e.target.value)} />
        </Field>
        <Field label="알림 시각" required error={errors.at}>
          <Input type="datetime-local" mono value={at} onChange={(e) => setAt(e.target.value)} />
        </Field>
        <Checkbox checked={sent} onChange={setSent} label="보냄" />
        <FormError error={update.error} />
      </form>
    </Modal>
  )
}
