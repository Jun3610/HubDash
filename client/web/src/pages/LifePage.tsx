import { useQueries } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { habitWeeklyStats } from '../api/analytics'
import { habits, readingLogs } from '../api/life'
import { BIG_PAGE, listKey, useCreate, useList, useRemove, useUpdate } from '../api/resource'
import type { Habit, HabitLog, HabitWeeklyStat, ReadingLog } from '../api/types'
import { PageHeader } from '../components/layout/PageHeader'
import {
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  Field,
  FormError,
  Input,
  Modal,
  QueryState,
  SectionHeader,
  Table,
  Tabs,
  Tag,
  Textarea,
} from '../components/ui'
import { pledgesStore } from '../config/prefs'
import { habitStreak, logOn, useHabitsWithLogs, useToggleHabit } from '../hooks/useLife'
import { useOptimistic } from '../hooks/useOptimistic'
import { useToday } from '../hooks/useToday'
import { formatShortDate, shiftDate, type LocalDate } from '../lib/date'
import { cellState, lastDays, readingState, recentRate, sortBooks, stars } from '../lib/select/life'
import { mergeWeekly } from '../lib/select/pknu'
import { createStore, useStore } from '../lib/storage'
import { hasErrors, maxLen, optStr, required, type Errors } from '../lib/validate'
import s from './life/Life.module.css'

/** "오늘 한 줄 메모" — 서버에 하루 메모 필드가 없어 날짜별로 이 브라우저에 저장 */
const dailyNoteStore = createStore<Record<string, string>>('hubdash.dailyNote', {})

type Tab = 'habits' | 'reading'

export default function LifePage() {
  const today = useToday()
  const [tab, setTab] = useState<Tab>('habits')
  const [habitDialog, setHabitDialog] = useState<{ habit?: Habit } | null>(null)
  const [bookDialog, setBookDialog] = useState<{ book?: ReadingLog } | null>(null)
  const life = useHabitsWithLogs()
  const books = useList(readingLogs, { size: BIG_PAGE, sort: 'startedAt,desc' })
  const bookList = sortBooks(books.data?.content ?? [])

  // 두 구역을 한 화면에 두고 탭은 해당 구역으로 이동 (디자인의 앵커 탭)
  const go = (k: Tab) => {
    setTab(k)
    document.getElementById(k)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  return (
    <>
      <PageHeader
        title="생활 · 습관"
        tabs={
          <Tabs<Tab>
            inHeader
            label="생활 탭"
            value={tab}
            onChange={go}
            items={[
              { key: 'habits', label: '습관', count: life.habits.length },
              { key: 'reading', label: '독서', count: bookList.length },
            ]}
          />
        }
      >
        <Button onClick={() => setBookDialog({})}>책 추가</Button>
        <Button variant="primary" icon={<Plus size={14} />} onClick={() => setHabitDialog({})}>
          습관 추가
        </Button>
      </PageHeader>

      <div className={s.layout}>
        <div className={s.main}>
          <Pledges />
          <section id="habits" className={s.box} aria-labelledby="tracker-title">
            <div className={s.boxHead}>
              <h2 id="tracker-title">습관 트래커</h2>
              <span className="muted" style={{ fontSize: 12 }}>
                최근 14일 · 칸을 누르면 그날 기록 토글
              </span>
              <span className={s.right}>
                {formatShortDate(shiftDate(today, -13))} → {formatShortDate(today)}
              </span>
            </div>
            <div className={life.habits.length ? undefined : s.pad}>
              <QueryState
                loading={life.isLoading && life.habits.length === 0}
                error={life.error}
                onRetry={life.refetch}
                empty={life.habits.length === 0}
                emptyView={
                  <EmptyState
                    title="등록한 습관이 없어요"
                    description="매일 체크할 습관을 추가해 보세요."
                    action={
                      <Button variant="primary" onClick={() => setHabitDialog({})}>
                        습관 추가
                      </Button>
                    }
                  />
                }
              >
                <Tracker
                  habits={life.habits}
                  logsByHabit={life.logsByHabit}
                  today={today}
                  onEdit={(habit) => setHabitDialog({ habit })}
                />
              </QueryState>
            </div>
          </section>
          <section id="reading" className={s.box} aria-labelledby="reading-title">
            <div className={s.boxHead}>
              <h2 id="reading-title">독서 기록</h2>
              <span className="muted" style={{ fontSize: 12 }}>
                올해 완독 {bookList.filter((b) => b.finishedAt?.startsWith(today.slice(0, 4))).length}권
              </span>
            </div>
            <div className={bookList.length ? undefined : s.pad}>
              <QueryState
                loading={books.isLoading}
                error={books.error}
                onRetry={() => void books.refetch()}
                empty={bookList.length === 0}
                emptyView={
                  <EmptyState
                    title="독서 기록이 없어요"
                    action={
                      <Button variant="primary" onClick={() => setBookDialog({})}>
                        책 추가
                      </Button>
                    }
                  />
                }
              >
                <Table>
                  <thead>
                    <tr>
                      <th scope="col">제목</th>
                      <th scope="col" className={s.hideMobile} style={{ width: 120 }}>
                        저자
                      </th>
                      <th scope="col" style={{ width: 150 }}>
                        기간
                      </th>
                      <th scope="col" style={{ width: 100 }}>
                        평점
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {bookList.map((b) => {
                      const st = readingState(b)
                      return (
                        <tr key={b.id}>
                          <td>
                            <button type="button" className={s.bookTitle} onClick={() => setBookDialog({ book: b })}>
                              {b.title}
                            </button>{' '}
                            <Tag tone={st === '완독' ? 'purple' : 'blue'}>{st}</Tag>
                          </td>
                          <td className={`${s.hideMobile} muted`}>{b.author ?? '—'}</td>
                          <td className="mono muted" style={{ fontSize: 12 }}>
                            {formatShortDate(b.startedAt)} → {b.finishedAt ? formatShortDate(b.finishedAt) : '—'}
                          </td>
                          <td className={s.stars} aria-label={b.rating ? `5점 만점에 ${b.rating}점` : '평점 없음'}>
                            {stars(b.rating)}
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </Table>
              </QueryState>
            </div>
          </section>
        </div>

        <aside className={s.aside}>
          <TodayCheck habits={life.habits} logsByHabit={life.logsByHabit} today={today} loading={life.isLoading} />
          <WeeklyRates habits={life.habits} />
          <ReadingNow book={bookList.find((b) => !b.finishedAt)} onEdit={(book) => setBookDialog({ book })} />
        </aside>
      </div>

      {habitDialog && <HabitModal habit={habitDialog.habit} onClose={() => setHabitDialog(null)} />}
      {bookDialog && <BookModal book={bookDialog.book} today={today} onClose={() => setBookDialog(null)} />}
    </>
  )
}

function Pledges() {
  const pledges = useStore(pledgesStore)
  return (
    <section className={s.pledges} aria-label="이번 학기 다짐">
      <span className="muted" style={{ fontSize: 12 }}>
        이번 학기 다짐
      </span>
      {pledges.length ? (
        <div className={s.pledgeList}>
          {pledges.map((p) => (
            <span key={p} className={s.pledge}>
              {p}
            </span>
          ))}
        </div>
      ) : (
        <span style={{ fontSize: 12.5 }}>
          아직 다짐이 없어요 · <Link to="/settings">설정에서 추가</Link>
        </span>
      )}
    </section>
  )
}

// ---- 14일 트래커 ----

function Tracker({
  habits: list,
  logsByHabit,
  today,
  onEdit,
}: {
  habits: Habit[]
  logsByHabit: Map<number, HabitLog[]>
  today: LocalDate
  onEdit: (h: Habit) => void
}) {
  const days = lastDays(today, 14)
  const { toggle } = useToggleHabit()
  const opt = useOptimistic<string>()
  return (
    <div className={s.trackerScroll}>
      <div className={`${s.row} ${s.headRow}`} aria-hidden="true">
        <span />
        {days.map((d) => (
          <span key={d} className={d === today ? s.todayLabel : undefined}>
            {d.slice(8)}
          </span>
        ))}
        <span style={{ textAlign: 'right', fontFamily: 'var(--font-sans)', fontSize: 11.5 }}>최근 7일</span>
      </div>
      {list.map((h) => {
        const logs = logsByHabit.get(h.id) ?? []
        const { done, total } = recentRate(logs, today)
        const streak = habitStreak(logs, today, shiftDate)
        return (
          <div key={h.id} className={s.row}>
            <button type="button" className={s.habitName} onClick={() => onEdit(h)} title="습관 수정">
              <span className="ellipsis">{h.name}</span>
              <span>{h.description ?? ''}</span>
            </button>
            {days.map((d) => {
              const key = `${h.id}:${d}`
              const server = cellState(logs, d, today)
              const isDone = opt.value(key, server === 'done')
              const state = isDone ? 'done' : server === 'done' ? 'missed' : server
              return (
                <button
                  key={d}
                  type="button"
                  className={s.cell}
                  data-state={state}
                  data-today={d === today}
                  aria-pressed={isDone}
                  aria-label={`${h.name} ${formatShortDate(d)} ${isDone ? '완료' : state === 'empty' ? '기록 없음' : '미완료'}`}
                  onClick={() => {
                    if (toggle(h, logs, d, { onSettled: () => opt.clear(key) })) opt.set(key, !isDone)
                  }}
                />
              )
            })}
            <div className={s.rate}>
              <span>
                {done}/{total}
              </span>
              <span>{streak ? `연속 ${streak}일` : '—'}</span>
            </div>
          </div>
        )
      })}
    </div>
  )
}

// ---- 오른쪽 ----

function TodayCheck({
  habits: list,
  logsByHabit,
  today,
  loading,
}: {
  habits: Habit[]
  logsByHabit: Map<number, HabitLog[]>
  today: LocalDate
  loading: boolean
}) {
  const { toggle } = useToggleHabit()
  const opt = useOptimistic<number>()
  const notes = useStore(dailyNoteStore)
  const isDone = (h: Habit) => opt.value(h.id, !!logOn(logsByHabit.get(h.id), today)?.completed)
  return (
    <Card>
      <SectionHeader
        title="오늘 체크"
        actions={
          <span className="mono muted" style={{ fontSize: 12 }}>
            {list.filter(isDone).length}/{list.length}
          </span>
        }
      />
      <QueryState
        loading={loading && list.length === 0}
        error={null}
        empty={list.length === 0}
        emptyView={<EmptyState compact title="습관이 없어요" />}
      >
        {list.map((h) => {
          const done = isDone(h)
          return (
            <label
              key={h.id}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                padding: '6px 0',
                borderTop: '1px solid var(--divider)',
                cursor: 'pointer',
              }}
            >
              <input
                type="checkbox"
                checked={done}
                style={{ width: 15, height: 15, margin: 0, accentColor: 'var(--accent-strong)' }}
                onChange={() => {
                  if (toggle(h, logsByHabit.get(h.id), today, { onSettled: () => opt.clear(h.id) }))
                    opt.set(h.id, !done)
                }}
              />
              <span style={{ flexGrow: 1, color: 'var(--text-strong)' }}>{h.name}</span>
            </label>
          )
        })}
      </QueryState>
      <Field label="오늘 한 줄 메모" hint="이 브라우저에 저장">
        <Input
          placeholder="예: 유산소 못 함 — 비 옴"
          value={notes[today] ?? ''}
          maxLength={200}
          onChange={(e) => dailyNoteStore.set((all) => ({ ...all, [today]: e.target.value }))}
        />
      </Field>
    </Card>
  )
}

function WeeklyRates({ habits: list }: { habits: Habit[] }) {
  const results = useQueries({
    queries: list.map((h) => {
      const q = { habitId: h.id, size: 20, sort: 'weekStart,desc' }
      return { queryKey: listKey(habitWeeklyStats.path, q), queryFn: () => habitWeeklyStats.list(q) }
    }),
  })
  const rows: HabitWeeklyStat[] = results.flatMap((r) => r.data?.content ?? [])
  const weeks = mergeWeekly(rows, 8)
  return (
    <Card>
      <SectionHeader
        title="주간 달성률"
        actions={
          <span className="muted" style={{ fontSize: 11.5 }}>
            habit-weekly-stats
          </span>
        }
      />
      <QueryState
        loading={results.some((r) => r.isLoading)}
        error={results.find((r) => r.error)?.error}
        empty={weeks.length === 0}
        emptyView={<EmptyState compact title="주간 통계가 아직 없어요" action={<Link to="/settings">계산</Link>} />}
      >
        <div className={s.rateBars} role="img" aria-label="주간 습관 달성률">
          {weeks.map((w) => {
            const p = Math.round(w.rate * 100)
            return (
              <div key={w.weekStart} className={s.rateCol} title={`${w.weekStart} 주 · ${w.done}/${w.total}`}>
                <span>{p}</span>
                <div style={{ height: `${p}%`, background: p >= 70 ? 'var(--heat-3)' : 'var(--heat-2)' }} />
              </div>
            )
          })}
        </div>
        <div className={s.axis}>
          <span>{formatShortDate(weeks[0]?.weekStart ?? '')}</span>
          <span>{formatShortDate(weeks[weeks.length - 1]?.weekStart ?? '')}</span>
        </div>
      </QueryState>
    </Card>
  )
}

function ReadingNow({ book, onEdit }: { book?: ReadingLog; onEdit: (b: ReadingLog) => void }) {
  return (
    <div
      style={{
        border: '1px solid var(--border)',
        borderRadius: 'var(--radius)',
        padding: '12px 14px',
        display: 'flex',
        gap: 12,
      }}
    >
      <div className={s.cover} aria-hidden="true" />
      <div style={{ display: 'flex', flexDirection: 'column', gap: 4, minWidth: 0 }}>
        <span className="muted" style={{ fontSize: 11.5 }}>
          읽는 중
        </span>
        {book ? (
          <>
            <button
              type="button"
              className={s.bookTitle}
              style={{ fontWeight: 600, color: 'var(--text)' }}
              onClick={() => onEdit(book)}
            >
              {book.title}
            </button>
            <span className="muted" style={{ fontSize: 12 }}>
              {book.author ?? '저자 미상'} · {formatShortDate(book.startedAt)} 시작
            </span>
            <span className="muted" style={{ fontSize: 12 }}>
              완독하면 평점과 메모를 남겨요
            </span>
          </>
        ) : (
          <span className="muted" style={{ fontSize: 12 }}>
            읽고 있는 책이 없어요
          </span>
        )}
      </div>
    </div>
  )
}

// ---- 모달 ----

function HabitModal({ habit, onClose }: { habit?: Habit; onClose: () => void }) {
  const [name, setName] = useState(habit?.name ?? '')
  const [description, setDescription] = useState(habit?.description ?? '')
  const [errors, setErrors] = useState<Errors>({})
  const [confirm, setConfirm] = useState(false)
  const create = useCreate(habits)
  const update = useUpdate(habits)
  const remove = useRemove(habits)
  const m = habit ? update : create
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = { name: required(name, '습관 이름') ?? maxLen(name, 100), description: maxLen(description, 500) }
    setErrors(errs)
    if (hasErrors(errs)) return
    const body = { name: name.trim(), description: optStr(description) }
    if (habit) update.mutate({ id: habit.id, body }, { onSuccess: onClose })
    else create.mutate(body, { onSuccess: onClose })
  }
  return (
    <Modal
      open
      onClose={onClose}
      title={habit ? '습관 수정' : '습관 추가'}
      footer={
        <>
          {habit && (
            <Button variant="danger" style={{ marginRight: 'auto' }} onClick={() => setConfirm(true)}>
              삭제
            </Button>
          )}
          <Button onClick={onClose}>취소</Button>
          <Button variant="primary" type="submit" form="habit-form" disabled={m.isPending}>
            저장
          </Button>
        </>
      }
    >
      <form id="habit-form" onSubmit={submit} noValidate style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <Field label="이름" required error={errors.name}>
          <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="물 2L 마시기" />
        </Field>
        <Field label="설명" error={errors.description}>
          <Input value={description} onChange={(e) => setDescription(e.target.value)} />
        </Field>
        <FormError error={m.error ?? remove.error} />
      </form>
      <ConfirmDialog
        open={confirm}
        title="습관 삭제"
        message="기록이 남아 있으면 서버가 거부할 수 있어요. 지울까요?"
        busy={remove.isPending}
        onClose={() => setConfirm(false)}
        onConfirm={() => habit && remove.mutate(habit.id, { onSuccess: onClose, onError: () => setConfirm(false) })}
      />
    </Modal>
  )
}

function BookModal({ book, today, onClose }: { book?: ReadingLog; today: LocalDate; onClose: () => void }) {
  const [d, setD] = useState({
    title: book?.title ?? '',
    author: book?.author ?? '',
    startedAt: book?.startedAt ?? today,
    finishedAt: book?.finishedAt ?? '',
    rating: book?.rating ?? 0,
    notes: book?.notes ?? '',
  })
  const [errors, setErrors] = useState<Errors>({})
  const [confirm, setConfirm] = useState(false)
  const create = useCreate(readingLogs)
  const update = useUpdate(readingLogs)
  const remove = useRemove(readingLogs)
  const m = book ? update : create
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = {
      title: required(d.title, '제목') ?? maxLen(d.title, 200),
      author: maxLen(d.author, 100),
      startedAt: required(d.startedAt, '시작일'),
      finishedAt: d.finishedAt && d.finishedAt < d.startedAt ? '완독일이 시작일보다 빨라요' : undefined,
      notes: maxLen(d.notes, 1000),
    }
    setErrors(errs)
    if (hasErrors(errs)) return
    const body = {
      title: d.title.trim(),
      author: optStr(d.author),
      startedAt: d.startedAt,
      finishedAt: d.finishedAt || null,
      rating: d.rating || null,
      notes: optStr(d.notes),
    }
    if (book) update.mutate({ id: book.id, body }, { onSuccess: onClose })
    else create.mutate(body, { onSuccess: onClose })
  }
  return (
    <Modal
      open
      onClose={onClose}
      title={book ? '독서 기록 수정' : '책 추가'}
      footer={
        <>
          {book && (
            <Button variant="danger" style={{ marginRight: 'auto' }} onClick={() => setConfirm(true)}>
              삭제
            </Button>
          )}
          <Button onClick={onClose}>취소</Button>
          <Button variant="primary" type="submit" form="book-form" disabled={m.isPending}>
            저장
          </Button>
        </>
      }
    >
      <form id="book-form" className={s.formGrid} onSubmit={submit} noValidate>
        <Field label="제목" required error={errors.title} className={s.full}>
          <Input value={d.title} onChange={(e) => setD({ ...d, title: e.target.value })} />
        </Field>
        <Field label="저자" error={errors.author} className={s.full}>
          <Input value={d.author} onChange={(e) => setD({ ...d, author: e.target.value })} />
        </Field>
        <Field label="시작일" required error={errors.startedAt}>
          <Input type="date" mono value={d.startedAt} onChange={(e) => setD({ ...d, startedAt: e.target.value })} />
        </Field>
        <Field label="완독일" error={errors.finishedAt} hint="비우면 읽는 중">
          <Input type="date" mono value={d.finishedAt} onChange={(e) => setD({ ...d, finishedAt: e.target.value })} />
        </Field>
        <div className={s.full} style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span className="muted" style={{ fontSize: 12 }} id="rating-label">
            평점
          </span>
          <div className={s.starPick} role="group" aria-labelledby="rating-label">
            {[1, 2, 3, 4, 5].map((n) => (
              <button
                key={n}
                type="button"
                aria-label={`${n}점`}
                aria-pressed={n <= d.rating}
                onClick={() => setD({ ...d, rating: d.rating === n ? 0 : n })}
              >
                ★
              </button>
            ))}
          </div>
        </div>
        <Field label="메모" error={errors.notes} className={s.full}>
          <Textarea rows={3} value={d.notes} onChange={(e) => setD({ ...d, notes: e.target.value })} />
        </Field>
        <div className={s.full}>
          <FormError error={m.error ?? remove.error} />
        </div>
      </form>
      <ConfirmDialog
        open={confirm}
        title="독서 기록 삭제"
        message={`"${book?.title}"을(를) 지울까요?`}
        busy={remove.isPending}
        onClose={() => setConfirm(false)}
        onConfirm={() => book && remove.mutate(book.id, { onSuccess: onClose })}
      />
    </Modal>
  )
}
