import { ChevronLeft, ChevronRight, Plus, Search, Trash2, X } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useCreate, useRemove, useUpdate } from '../api/resource'
import { events } from '../api/schedule'
import type { ScheduleEvent } from '../api/types'
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
  Segmented,
  Textarea,
  TimeField,
} from '../components/ui'
import miscStyles from '../components/ui/Misc.module.css'
import { useAllEvents } from '../hooks/useEvents'
import { useNow, useToday } from '../hooks/useToday'
import { useUrlState } from '../hooks/useUrlState'
import {
  datePart,
  daysBetween,
  formatShortDate,
  formatTime,
  shiftDate,
  toLocalDateTime,
  weekDays,
  weekdayKo,
  type LocalDate,
} from '../lib/date'
import {
  allDayOn,
  eventsOn,
  GRID_END_HOUR,
  GRID_START_HOUR,
  monthGrid,
  placeDay,
  searchEvents,
  timeRange,
  upcoming,
} from '../lib/select/schedule'
import { hasErrors, maxLen, optStr, required, type Errors } from '../lib/validate'
import { overlapsDate, sortBy } from '../lib/select/range'
import s from './schedule/Schedule.module.css'

type View = 'month' | 'week' | 'list'
const HOUR_PX = 56
const PX_PER_MIN = HOUR_PX / 60
const HOURS = Array.from(
  { length: GRID_END_HOUR - GRID_START_HOUR },
  (_, i) => `${String(GRID_START_HOUR + i).padStart(2, '0')}:00`,
)

export default function SchedulePage() {
  const today = useToday()
  const now = toLocalDateTime(useNow())
  const [view, setView] = useUrlState('view', 'week')
  const [anchor, setAnchor] = useUrlState('date', today)
  const [selectedId, setSelectedId] = useUrlState('event', '')
  const [params, setParams] = useSearchParams()
  const [dialog, setDialog] = useState<{ event?: ScheduleEvent; date?: LocalDate } | null>(null)
  const [q, setQ] = useState('')
  const list = useAllEvents()
  const all = list.data?.content ?? []
  const selected = all.find((e) => String(e.id) === selectedId) ?? null

  // 빠른 기록(?new=1)으로 들어오면 추가 창
  const quickNew = params.get('new') === '1'
  const active = dialog ?? (quickNew ? {} : null)
  const closeDialog = () => {
    setDialog(null)
    if (quickNew)
      setParams(
        (p) => {
          const n = new URLSearchParams(p)
          n.delete('new')
          return n
        },
        { replace: true },
      )
  }

  const step = view === 'month' ? 'month' : view === 'week' ? 7 : 'month'
  const move = (dir: 1 | -1) => {
    if (step === 7) setAnchor(shiftDate(anchor, 7 * dir))
    else {
      const y = Number(anchor.slice(0, 4))
      const m = Number(anchor.slice(5, 7)) - 1 + dir
      const d = new Date(y, m, 1)
      setAnchor(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`)
    }
  }
  // 검색은 지금 보기 안의 필터: List는 전체, W는 그 주, M은 그 달에서 맞는 일정만 (이슈 #185)
  const searching = q.trim() !== ''
  const total = list.data?.totalElements ?? all.length
  const shown = searching ? searchEvents(all, q) : all
  const days = weekDays(anchor)
  const inRange =
    view === 'week'
      ? shown.filter((e) => days.some((d) => overlapsDate(e.startAt, e.endAt, d))).length
      : view === 'month'
        ? shown.filter((e) => datePart(e.startAt).slice(0, 7) === anchor.slice(0, 7)).length
        : shown.length
  const rangeLabel =
    view === 'week'
      ? `${days[0].replaceAll('-', '.')} – ${formatShortDate(days[6])}`
      : `${anchor.slice(0, 4)}.${anchor.slice(5, 7)}`

  return (
    <>
      <PageHeader
        title="Schedule"
        tabs={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Segmented<View>
              label="보기"
              value={view as View}
              onChange={setView}
              items={[
                { key: 'month', label: 'M', title: '월' },
                { key: 'week', label: 'W', title: '주' },
                { key: 'list', label: 'L', title: '전체 일정 (최신순)' },
              ]}
            />
            {/* List는 전체 일정이라 날짜 이동이 필요 없다 (이슈 #177) */}
            {view !== 'list' && (
              <div className={miscStyles.dateNav}>
                <button
                  type="button"
                  className={miscStyles.navBtn}
                  aria-label={view === 'week' ? '이전 주' : '이전 달'}
                  onClick={() => move(-1)}
                >
                  <ChevronLeft size={14} />
                </button>
                <button type="button" className={miscStyles.todayBtn} onClick={() => setAnchor(today)}>
                  Today
                </button>
                <button
                  type="button"
                  className={miscStyles.navBtn}
                  aria-label={view === 'week' ? '다음 주' : '다음 달'}
                  onClick={() => move(1)}
                >
                  <ChevronRight size={14} />
                </button>
                <span className={miscStyles.dateLabel} aria-live="polite">
                  {rangeLabel}
                </span>
              </div>
            )}
          </div>
        }
      >
        <label className={s.search}>
          <Search size={14} />
          <input
            type="text"
            placeholder="Search"
            aria-label="일정 검색"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Escape' && q) {
                e.stopPropagation()
                setQ('')
              }
            }}
          />
          {q && (
            <IconButton label="검색어 지우기" size="sm" onClick={() => setQ('')}>
              <X size={13} />
            </IconButton>
          )}
        </label>
        <Button variant="primary" icon={<Plus size={14} />} onClick={() => setDialog({ date: anchor })}>
          Add Schedule
        </Button>
      </PageHeader>

      <div className={s.layout}>
        <section
          aria-label={view === 'week' ? '주간 캘린더' : view === 'month' ? '월간 캘린더' : '일정 목록'}
          className={s.cal}
        >
          <QueryState loading={list.isLoading} error={list.error} onRetry={() => void list.refetch()} lines={8}>
            {/* 개수 줄: 검색하지 않아도 이 주·이 달·전체 개수를 보여 준다 (이슈 #185) */}
            <div className={s.searchBar} role="status">
              {searching && <span className={s.searchWord}>"{q.trim()}"</span>}
              {view === 'week' ? '이 주' : view === 'month' ? '이 달' : searching ? '검색' : '총'}{' '}
              <b>{inRange.toLocaleString()}</b>
              {view === 'list' && !searching ? '개 일정' : '건'}
              {/* List는 전체라 DB 개수와 겹치지 않게: 검색 안 하면 생략, 검색하면 DB 개수만 */}
              {!(view === 'list' && !searching) && (
                <span className="muted">
                  {' '}
                  · {searching && view !== 'list' ? `검색 전체 ${shown.length}건 / ` : ''}DB 전체{' '}
                  {total.toLocaleString()}개
                </span>
              )}
              {searching && (
                <button type="button" onClick={() => setQ('')}>
                  검색 지우기
                </button>
              )}
            </div>
            {view === 'week' && (
              <WeekView
                days={days}
                today={today}
                now={now}
                events={shown}
                selectedId={selected?.id}
                onSelect={(e) => setSelectedId(String(e.id))}
              />
            )}
            {view === 'month' && (
              <MonthView
                anchor={anchor}
                today={today}
                events={shown}
                selectedId={selected?.id}
                onSelect={(e) => setSelectedId(String(e.id))}
                onDay={(d) => {
                  setAnchor(d)
                  setView('week')
                }}
              />
            )}
            {view === 'list' && (
              <ListView
                today={today}
                events={shown}
                searching={searching}
                selectedId={selected?.id}
                onSelect={(e) => setSelectedId(String(e.id))}
              />
            )}
          </QueryState>
        </section>

        <aside className={s.aside}>
          {selected ? (
            <Detail
              event={selected}
              onEdit={() => setDialog({ event: selected })}
              onDeleted={() => setSelectedId(null)}
            />
          ) : (
            <div className={s.box} style={{ padding: 14 }}>
              <span className="muted" style={{ fontSize: 12.5 }}>
                Click a schedule to see the details here.
              </span>
            </div>
          )}
          <section className={s.box} aria-label="다가오는 일정">
            <div className={s.boxHead}>
              <h2>Coming Schedule</h2>
              <span className="muted" style={{ marginLeft: 'auto', fontSize: 12 }}>
                7 days
              </span>
            </div>
            <UpcomingList
              events={all}
              now={now}
              onSelect={(e) => {
                setSelectedId(String(e.id))
                setAnchor(datePart(e.startAt))
              }}
            />
          </section>
        </aside>
      </div>

      {active && (
        <EventModal
          event={active.event}
          date={active.date ?? anchor}
          onClose={closeDialog}
          onSaved={(e) => setSelectedId(String(e.id))}
        />
      )}
    </>
  )
}

// ---- 주 보기 ----

function WeekView({
  days,
  today,
  now,
  events: list,
  selectedId,
  onSelect,
}: {
  days: LocalDate[]
  today: LocalDate
  now: string
  events: ScheduleEvent[]
  selectedId?: number
  onSelect: (e: ScheduleEvent) => void
}) {
  const nowMin = Number(now.slice(11, 13)) * 60 + Number(now.slice(14, 16)) - GRID_START_HOUR * 60
  const showNow = nowMin >= 0 && nowMin <= (GRID_END_HOUR - GRID_START_HOUR) * 60
  return (
    <>
      <div className={`${s.weekRow} ${s.dayHead}`}>
        <span />
        {days.map((d, i) => (
          <div key={d} data-today={d === today} data-weekend={i >= 5}>
            <span className={s.dow}>{weekdayKo(d)}</span>
            <span className={s.num}>{Number(d.slice(8))}</span>
          </div>
        ))}
      </div>
      <div className={`${s.weekRow} ${s.allDayRow}`}>
        <span className={s.allDayLabel}>종일</span>
        {days.map((d) => (
          <div key={d} className={s.allDayCell}>
            {allDayOn(list, d).map((e) => (
              <button
                key={e.id}
                type="button"
                className={s.allDayChip}
                aria-pressed={e.id === selectedId}
                onClick={() => onSelect(e)}
                title={e.title}
              >
                {e.title}
              </button>
            ))}
          </div>
        ))}
      </div>
      <div className={s.body}>
        <div className={s.weekRow}>
          <div className={s.hours} aria-hidden="true">
            {HOURS.map((h) => (
              <span key={h}>{h}</span>
            ))}
          </div>
          {days.map((d) => (
            <div key={d} className={s.col} data-today={d === today}>
              {HOURS.map((h) => (
                <div key={h} className={s.slot} />
              ))}
              {placeDay(list, d).map((p) => {
                const w = 100 / p.lanes
                return (
                  <button
                    key={p.event.id}
                    type="button"
                    className={s.event}
                    // 30분 이하 칸은 "10:00 제목" 한 줄로
                    data-short={p.height <= 30}
                    aria-pressed={p.event.id === selectedId}
                    onClick={() => onSelect(p.event)}
                    title={`${p.event.title} ${timeRange(p.event)}`}
                    style={{
                      top: p.top * PX_PER_MIN,
                      height: Math.max(p.height * PX_PER_MIN - 2, 18),
                      left: `calc(${p.lane * w}% + 3px)`,
                      width: `calc(${w}% - 6px)`,
                      borderTopStyle: p.clippedTop ? 'dashed' : undefined,
                      borderBottomStyle: p.clippedBottom ? 'dashed' : undefined,
                    }}
                  >
                    {p.height <= 30 && <span className={s.eventTime}>{timeRange(p.event)}</span>}
                    <span className={s.eventTitle}>{p.event.title}</span>
                    {p.height > 30 && <span className={s.eventTime}>{timeRange(p.event)}</span>}
                    {p.event.location && <span className={s.eventLoc}>{p.event.location}</span>}
                  </button>
                )
              })}
              {d === today && showNow && (
                <div
                  className={s.nowLine}
                  style={{ top: nowMin * PX_PER_MIN }}
                  aria-label={`현재 시각 ${now.slice(11, 16)}`}
                />
              )}
            </div>
          ))}
        </div>
      </div>
    </>
  )
}

// ---- 월 보기 ----

function MonthView({
  anchor,
  today,
  events: list,
  selectedId,
  onSelect,
  onDay,
}: {
  anchor: LocalDate
  today: LocalDate
  events: ScheduleEvent[]
  selectedId?: number
  onSelect: (e: ScheduleEvent) => void
  onDay: (d: LocalDate) => void
}) {
  const grid = monthGrid(anchor)
  const month = anchor.slice(0, 7)
  return (
    <div className={s.monthGrid}>
      {['월', '화', '수', '목', '금', '토', '일'].map((d) => (
        <div key={d} className={s.monthHead}>
          {d}
        </div>
      ))}
      {grid.map((d) => {
        const on = eventsOn(list, d)
        return (
          <div key={d} className={s.monthCell} data-out={!d.startsWith(month)} data-today={d === today}>
            <button type="button" className={s.monthNum} onClick={() => onDay(d)} aria-label={`${d} 주 보기`}>
              {Number(d.slice(8))}
            </button>
            {on.slice(0, 3).map((e) => (
              <button
                key={e.id}
                type="button"
                className={s.monthItem}
                aria-pressed={e.id === selectedId}
                onClick={() => onSelect(e)}
                title={e.title}
              >
                <span className={s.t}>{e.allDay ? '종일' : formatTime(e.startAt)}</span>
                {e.title}
              </button>
            ))}
            {on.length > 3 && <span className={s.more}>+{on.length - 3}</span>}
          </div>
        )
      })}
    </div>
  )
}

// ---- 목록 보기 (그 달) ----

/** List: 그동안의 일정 전부, 최신(시작일이 늦은 날)이 맨 위. 하루 안에서는 종일 → 시각 순 (이슈 #177) */
function ListView({
  today,
  events: list,
  searching,
  selectedId,
  onSelect,
}: {
  today: LocalDate
  events: ScheduleEvent[]
  searching: boolean
  selectedId?: number
  onSelect: (e: ScheduleEvent) => void
}) {
  const byDay = new Map<LocalDate, ScheduleEvent[]>()
  for (const e of list) {
    const d = datePart(e.startAt)
    byDay.set(d, [...(byDay.get(d) ?? []), e])
  }
  const rows = [...byDay.entries()]
    .sort((a, b) => (a[0] < b[0] ? 1 : -1))
    .map(([d, on]) => ({ d, on: sortBy(on, (e) => (e.allDay ? '0' : '1') + e.startAt) }))
  if (rows.length === 0) return <EmptyState title={searching ? '찾는 일정이 없어요' : '일정이 없어요'} />
  return (
    <div>
      {rows.map(({ d, on }) => (
        <div key={d} className={s.listDay} data-today={d === today}>
          <span className={s.listDate} data-today={d === today}>
            {d.slice(0, 4) !== today.slice(0, 4) && `${d.slice(2, 4)}.`}
            {formatShortDate(d)} {weekdayKo(d)}
          </span>
          <div>
            {on.map((e) => (
              <button
                key={e.id}
                type="button"
                className={s.listItem}
                aria-pressed={e.id === selectedId}
                onClick={() => onSelect(e)}
              >
                <span className="mono muted" style={{ fontSize: 12 }}>
                  {e.allDay ? '종일' : timeRange(e)}
                </span>
                <span className="ellipsis" style={{ color: 'var(--text-strong)' }}>
                  {e.title}
                  {e.location && <span className="muted"> · {e.location}</span>}
                </span>
              </button>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}

// ---- 오른쪽 ----

function Detail({ event, onEdit, onDeleted }: { event: ScheduleEvent; onEdit: () => void; onDeleted: () => void }) {
  const [confirm, setConfirm] = useState(false)
  const remove = useRemove(events)
  const sd = datePart(event.startAt)
  const ed = datePart(event.endAt ?? event.startAt)
  const time = event.allDay
    ? sd === ed
      ? `${formatShortDate(sd)} ${weekdayKo(sd)} 종일`
      : `${formatShortDate(sd)} – ${formatShortDate(ed)} 종일`
    : !event.endAt
      ? `${formatShortDate(sd)} ${weekdayKo(sd)} ${formatTime(event.startAt)}`
      : sd === ed
        ? `${formatShortDate(sd)} ${weekdayKo(sd)} ${formatTime(event.startAt)} – ${formatTime(event.endAt)}`
        : `${formatShortDate(sd)} ${formatTime(event.startAt)} – ${formatShortDate(ed)} ${formatTime(event.endAt)}`
  return (
    <section className={s.detail} aria-label="선택한 일정">
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <span style={{ fontSize: 11.5, color: 'var(--accent)' }}>선택한 일정</span>
        <Button size="sm" style={{ marginLeft: 'auto' }} onClick={onEdit}>
          수정
        </Button>
        <IconButton
          label="삭제"
          size="sm"
          style={{ color: 'var(--red)', border: '1px solid var(--border)', background: 'var(--fill)' }}
          onClick={() => setConfirm(true)}
        >
          <Trash2 size={13} />
        </IconButton>
      </div>
      <h2>{event.title}</h2>
      <dl className={s.dl}>
        <dt>시간</dt>
        <dd className="mono">{time}</dd>
        <dt>장소</dt>
        <dd>{event.location ?? '—'}</dd>
        <dt>종일</dt>
        <dd>{event.allDay ? '예' : '아니요'}</dd>
        <dt>메모</dt>
        <dd style={{ color: 'var(--text-body)', whiteSpace: 'pre-wrap' }}>{event.description ?? '—'}</dd>
      </dl>
      <ConfirmDialog
        open={confirm}
        title="일정 삭제"
        message={`"${event.title}"을(를) 지울까요?`}
        busy={remove.isPending}
        onClose={() => setConfirm(false)}
        onConfirm={() =>
          remove.mutate(event.id, {
            onSuccess: () => {
              setConfirm(false)
              onDeleted()
            },
          })
        }
      />
    </section>
  )
}

function UpcomingList({
  events: list,
  now,
  onSelect,
}: {
  events: ScheduleEvent[]
  now: string
  onSelect: (e: ScheduleEvent) => void
}) {
  const items = upcoming(list, now, 7).slice(0, 8)
  if (items.length === 0)
    return (
      <div style={{ padding: '10px 14px' }} className="muted">
        7일 안에 일정이 없어요
      </div>
    )
  return (
    <>
      {items.map((e) => (
        <button key={e.id} type="button" className={s.up} onClick={() => onSelect(e)}>
          <div className={s.upWhen}>
            <span>{formatShortDate(datePart(e.startAt))}</span>
            <span>{e.allDay ? '종일' : formatTime(e.startAt)}</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
            <span className="ellipsis" style={{ color: 'var(--text-strong)' }}>
              {e.title}
            </span>
            {e.location && (
              <span className="muted ellipsis" style={{ fontSize: 11.5 }}>
                {e.location}
              </span>
            )}
          </div>
        </button>
      ))}
    </>
  )
}

// ---- 추가·수정 ----

function EventModal({
  event,
  date,
  onClose,
  onSaved,
}: {
  event?: ScheduleEvent
  date: LocalDate
  onClose: () => void
  onSaved: (e: ScheduleEvent) => void
}) {
  // 여러 날에 걸친 기존 일정(추석 연휴 등)은 기간을 그대로 둔다. 새 일정은 시작 + 필요하면 끝 시각만 (이슈 #156)
  const span = event?.endAt ? daysBetween(datePart(event.startAt), datePart(event.endAt)) : 0
  const [d, setD] = useState({
    title: event?.title ?? '',
    allDay: event?.allDay ?? false,
    startDate: event ? datePart(event.startAt) : date,
    startTime: event && !event.allDay ? formatTime(event.startAt) : '10:00',
    hasEnd: !!event?.endAt && !event.allDay,
    endTime: event?.endAt && !event.allDay ? formatTime(event.endAt) : '11:00',
    location: event?.location ?? '',
    description: event?.description ?? '',
  })
  const [errors, setErrors] = useState<Errors>({})
  const create = useCreate(events)
  const update = useUpdate(events)
  const m = event ? update : create
  const startAt = `${d.startDate}T${d.allDay ? '00:00' : d.startTime}:00`
  // 끝 시각이 시작보다 이르거나 같으면 다음 날로 본다 (23:00 → 01:00)
  const endNextDay = !d.allDay && d.hasEnd && span === 0 && d.endTime <= d.startTime
  const endAt = d.allDay
    ? span > 0
      ? `${shiftDate(d.startDate, span)}T${event!.endAt!.slice(11, 19)}`
      : null
    : d.hasEnd
      ? `${shiftDate(d.startDate, span || (endNextDay ? 1 : 0))}T${d.endTime}:00`
      : span > 0
        ? `${shiftDate(d.startDate, span)}T${event!.endAt!.slice(11, 19)}`
        : null
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = {
      title: required(d.title, '제목') ?? maxLen(d.title, 200),
      startDate:
        required(d.startDate, '날짜') ?? (endAt && endAt <= startAt ? '끝나는 시각이 시작보다 늦어야 해요' : undefined),
      location: maxLen(d.location, 200),
      description: maxLen(d.description, 2000),
    }
    setErrors(errs)
    if (hasErrors(errs)) return
    const body = {
      title: d.title.trim(),
      startAt,
      endAt,
      allDay: d.allDay,
      location: optStr(d.location),
      description: optStr(d.description),
    }
    const done = (x: ScheduleEvent) => {
      onSaved(x)
      onClose()
    }
    if (event) update.mutate({ id: event.id, body }, { onSuccess: done })
    else create.mutate(body, { onSuccess: done })
  }
  const set = (k: keyof typeof d) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setD({ ...d, [k]: e.target.value })
  return (
    <Modal
      open
      onClose={onClose}
      title={event ? '일정 수정' : '일정 추가'}
      footer={
        <>
          <Button onClick={onClose}>취소</Button>
          <Button variant="primary" type="submit" form="event-form" disabled={m.isPending}>
            저장
          </Button>
        </>
      }
    >
      <form id="event-form" className={s.formGrid} onSubmit={submit} noValidate>
        <Field label="제목" required error={errors.title} className={s.full}>
          <Input value={d.title} onChange={set('title')} />
        </Field>
        <div className={s.full}>
          <Checkbox checked={d.allDay} onChange={(v) => setD({ ...d, allDay: v })} label="종일" />
        </div>
        <Field label="날짜" required error={errors.startDate}>
          <Input type="date" mono value={d.startDate} onChange={set('startDate')} />
        </Field>
        {!d.allDay ? (
          <Field label="시작 시각" required>
            <TimeField value={d.startTime} onChange={(v) => setD({ ...d, startTime: v })} />
          </Field>
        ) : (
          <span />
        )}
        {!d.allDay &&
          (d.hasEnd ? (
            <Field
              label="끝 시각"
              hint={endNextDay ? '시작보다 이르면 다음 날로 저장돼요' : undefined}
              className={s.full}
            >
              <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                <div style={{ flex: '0 1 50%' }}>
                  <TimeField value={d.endTime} onChange={(v) => setD({ ...d, endTime: v })} />
                </div>
                <Button size="sm" variant="link" onClick={() => setD({ ...d, hasEnd: false })}>
                  끝 시각 빼기
                </Button>
              </div>
            </Field>
          ) : (
            <div className={s.full}>
              <Button size="sm" icon={<Plus size={13} />} onClick={() => setD({ ...d, hasEnd: true })}>
                끝 시각 추가
              </Button>
            </div>
          ))}
        {span > 0 && (
          <span className={`${s.full} muted`} style={{ fontSize: 12 }}>
            여러 날 일정이에요 · {formatShortDate(shiftDate(d.startDate, span))}까지 (날짜를 바꾸면 기간째 옮겨져요)
          </span>
        )}
        <Field label="장소" error={errors.location} className={s.full}>
          <Input value={d.location} onChange={set('location')} />
        </Field>
        <Field label="메모" error={errors.description} className={s.full}>
          <Textarea rows={3} value={d.description} onChange={set('description')} />
        </Field>
        <div className={s.full}>
          <FormError error={m.error} />
        </div>
      </form>
    </Modal>
  )
}
