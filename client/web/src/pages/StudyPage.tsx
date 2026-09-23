import { useQueries } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { studyWeeklyStats } from '../api/analytics'
import { BIG_PAGE, listKey, useCreate, useList, useListsByParent, useRemove, useUpdate } from '../api/resource'
import { studyProgresses, studyTopics } from '../api/study'
import type { StudyProgress, StudyTopic, StudyTopicWeeklyStat } from '../api/types'
import { PageHeader } from '../components/layout/PageHeader'
import {
  Button,
  ConfirmDialog,
  EmptyState,
  Field,
  FormError,
  HBarList,
  Input,
  Modal,
  QueryState,
  RowActions,
  Select,
  Table,
  Tabs,
  Tag,
  Textarea,
  barVar,
  useToast,
  type BarColor,
  type Tone,
} from '../components/ui'
import { YEAR_PAGE } from '../hooks/useActivity'
import { useToday } from '../hooks/useToday'
import { useUrlState } from '../hooks/useUrlState'
import { formatMinutes, formatShortDate, shiftDate, weekStartOf, type LocalDate } from '../lib/date'
import { sortBy, withinDates } from '../lib/select/range'
import { longestStreak, studyStreak, weeklyMinutes } from '../lib/select/study'
import { hasErrors, maxLen, numRange, optStr, required, type Errors } from '../lib/validate'
import s from './study/Study.module.css'

/** 주제 순서대로 쓰는 색 (점, 라벨, 막대가 같은 색) */
const TOPIC_COLORS: (BarColor & Tone)[] = ['green', 'orange', 'purple', 'blue', 'yellow', 'accent', 'red']
const colorOf = (i: number) => TOPIC_COLORS[i % TOPIC_COLORS.length]

type Tab = 'topics' | 'logs' | 'stats'

function useStudyData() {
  const topics = useList(studyTopics, { size: BIG_PAGE, sort: 'createdAt,asc' })
  const list = useMemo(() => topics.data?.content ?? [], [topics.data])
  const ids = useMemo(() => list.map((t) => t.id), [list])
  // 홈·히트맵과 같은 쿼리를 써서 캐시를 공유
  const prog = useListsByParent<StudyProgress>(studyProgresses, 'topicId', ids, {
    size: YEAR_PAGE,
    sort: 'studiedAt,desc',
  })
  return {
    topics: list,
    progresses: prog.data,
    byTopic: prog.byParent,
    isLoading: topics.isLoading || prog.isLoading,
    error: topics.error ?? prog.error,
    refetch: () => {
      void topics.refetch()
      void prog.refetch()
    },
  }
}

export default function StudyPage() {
  const today = useToday()
  const [tab, setTab] = useUrlState('tab', 'topics')
  const [params] = useSearchParams()
  const [topicDialog, setTopicDialog] = useState<{ topic?: StudyTopic } | null>(null)
  const [editLog, setEditLog] = useState<StudyProgress | null>(null)
  const formRef = useRef<HTMLSelectElement>(null)
  const data = useStudyData()
  const colorIdx = new Map(data.topics.map((t, i) => [t.id, i]))

  return (
    <>
      <PageHeader
        title="공부"
        tabs={
          <Tabs<Tab>
            inHeader
            label="공부 탭"
            value={tab as Tab}
            onChange={setTab}
            items={[
              { key: 'topics', label: '주제', count: data.topics.length },
              { key: 'logs', label: '기록', count: data.progresses.length },
              { key: 'stats', label: '주간 통계' },
            ]}
          />
        }
      >
        <Button onClick={() => setTopicDialog({})}>주제 추가</Button>
        <Button
          variant="primary"
          icon={<Plus size={14} />}
          onClick={() => formRef.current?.focus()}
          disabled={!data.topics.length}
        >
          공부 기록
        </Button>
      </PageHeader>

      <div className={s.layout}>
        <section className={s.main} aria-label="공부 본문">
          <QueryState
            loading={data.isLoading && data.topics.length === 0}
            error={data.error}
            onRetry={data.refetch}
            empty={data.topics.length === 0}
            emptyView={
              <div className={s.box}>
                <EmptyState
                  title="공부 주제가 없어요"
                  description="주제를 만들고 공부한 시간을 기록해 보세요."
                  action={
                    <Button variant="primary" onClick={() => setTopicDialog({})}>
                      주제 추가
                    </Button>
                  }
                />
              </div>
            }
          >
            {tab === 'topics' && (
              <>
                <Kpis progresses={data.progresses} topics={data.topics} today={today} />
                <section style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  <div className={s.sectionHead}>
                    <h2>주제</h2>
                    <span className="muted" style={{ fontSize: 12 }}>
                      막대 = 최근 8주 공부 시간
                    </span>
                  </div>
                  <div className={s.topics}>
                    {data.topics.map((t, i) => (
                      <TopicCard
                        key={t.id}
                        topic={t}
                        color={colorOf(i)}
                        list={data.byTopic.get(t.id) ?? []}
                        today={today}
                        onEdit={() => setTopicDialog({ topic: t })}
                      />
                    ))}
                  </div>
                </section>
                <LogTable
                  title="최근 기록"
                  list={sortBy(data.progresses, (p) => p.studiedAt + p.createdAt, 'desc').slice(0, 8)}
                  topics={data.topics}
                  colorIdx={colorIdx}
                  onEdit={setEditLog}
                />
              </>
            )}
            {tab === 'logs' && (
              <LogTable
                title="전체 기록"
                list={sortBy(data.progresses, (p) => p.studiedAt + p.createdAt, 'desc')}
                topics={data.topics}
                colorIdx={colorIdx}
                onEdit={setEditLog}
              />
            )}
            {tab === 'stats' && <WeeklyStatsTable topics={data.topics} colorIdx={colorIdx} />}
          </QueryState>
        </section>

        <aside className={s.aside} aria-label="빠른 기록">
          <QuickForm
            key={params.get('new') ?? 'form'}
            topics={data.topics}
            today={today}
            selectRef={formRef}
            autoFocus={params.get('new') === '1'}
          />
          <WeekSplit topics={data.topics} byTopic={data.byTopic} today={today} />
        </aside>
      </div>

      {topicDialog && <TopicModal topic={topicDialog.topic} onClose={() => setTopicDialog(null)} />}
      {editLog && <ProgressModal log={editLog} topics={data.topics} onClose={() => setEditLog(null)} />}
    </>
  )
}

// ---- KPI ----

function Kpis({ progresses, topics, today }: { progresses: StudyProgress[]; topics: StudyTopic[]; today: LocalDate }) {
  const from = weekStartOf(today)
  const week = withinDates(progresses, (p) => p.studiedAt, from, shiftDate(from, 6))
  const last = withinDates(progresses, (p) => p.studiedAt, shiftDate(from, -7), shiftDate(from, -1))
  const mins = week.reduce((a, p) => a + p.minutes, 0)
  const lastMins = last.reduce((a, p) => a + p.minutes, 0)
  const diff = mins - lastMins
  const active = new Set(
    withinDates(progresses, (p) => p.studiedAt, shiftDate(today, -27), today).map((p) => p.topicId),
  ).size
  const kpis = [
    {
      label: '이번 주',
      value: formatMinutes(mins),
      sub: `지난주 ${diff >= 0 ? '+' : '−'}${formatMinutes(Math.abs(diff))}`,
    },
    { label: '세션', value: String(week.length), sub: week.length ? `평균 ${Math.round(mins / week.length)}분` : '—' },
    {
      label: '연속 기록',
      value: `${studyStreak(progresses, today)}일`,
      sub: `최장 ${longestStreak(progresses.map((p) => p.studiedAt))}일`,
    },
    { label: '주제', value: String(topics.length), sub: `활성 ${active}` },
  ]
  return (
    <section aria-label="이번 주 요약" className={s.kpis}>
      {kpis.map((k) => (
        <div key={k.label} className={s.kpi}>
          <span>{k.label}</span>
          <div>
            <b>{k.value}</b>
            <span>{k.sub}</span>
          </div>
        </div>
      ))}
    </section>
  )
}

// ---- 주제 카드 ----

function TopicCard({
  topic,
  color,
  list,
  today,
  onEdit,
}: {
  topic: StudyTopic
  color: BarColor
  list: StudyProgress[]
  today: LocalDate
  onEdit: () => void
}) {
  const weeks = weeklyMinutes(list, today, 8)
  const max = Math.max(1, ...weeks.map((w) => w.minutes))
  const total = list.reduce((a, p) => a + p.minutes, 0)
  const thisWeek = weeks[weeks.length - 1]
  const sessions = list.filter((p) => weekStartOf(p.studiedAt) === thisWeek.weekStart).length
  return (
    <article className={s.topic}>
      <div className={s.topicHead}>
        <span className={s.dot} style={{ background: barVar(color) }} />
        <button type="button" className={s.topicName} onClick={onEdit} title="주제 수정">
          {topic.name}
        </button>
        <span className={s.total}>{total >= 60 ? `${Math.round(total / 60)}h` : `${total}m`}</span>
      </div>
      <span className={s.desc}>{topic.description ?? ''}</span>
      <div className={s.spark} role="img" aria-label={`${topic.name} 최근 8주 공부 시간`}>
        {weeks.map((w, i) => (
          <span
            key={w.weekStart}
            title={`${w.weekStart} 주 · ${formatMinutes(w.minutes)}`}
            style={{
              height: `${Math.max(8, (w.minutes / max) * 100)}%`,
              background: i === weeks.length - 1 ? barVar(color) : undefined,
            }}
          />
        ))}
      </div>
      <div className={s.topicFoot}>
        <span>
          이번 주 <b>{formatMinutes(thisWeek.minutes)}</b>
        </span>
        <span>{sessions ? `${sessions} 세션` : '—'}</span>
      </div>
    </article>
  )
}

// ---- 기록 표 ----

function LogTable({
  title,
  list,
  topics,
  colorIdx,
  onEdit,
}: {
  title: string
  list: StudyProgress[]
  topics: StudyTopic[]
  colorIdx: Map<number, number>
  onEdit: (p: StudyProgress) => void
}) {
  const remove = useRemove(studyProgresses)
  const [del, setDel] = useState<StudyProgress | null>(null)
  const name = new Map(topics.map((t) => [t.id, t.name]))
  return (
    <section className={s.box}>
      <div className={s.boxHead}>
        <h2>{title}</h2>
        <span className="mono muted" style={{ fontSize: 12 }}>
          study/progresses
        </span>
      </div>
      {list.length === 0 ? (
        <div className={s.pad}>
          <EmptyState compact title="아직 기록이 없어요 — 오른쪽 빠른 기록으로 추가해 보세요" />
        </div>
      ) : (
        <Table>
          <thead>
            <tr>
              <th scope="col" style={{ width: 80 }}>
                날짜
              </th>
              <th scope="col" style={{ width: 150 }}>
                주제
              </th>
              <th scope="col" className="num" style={{ width: 60 }}>
                분
              </th>
              <th scope="col" className={s.hideMobile}>
                메모
              </th>
              <th scope="col" style={{ width: 60 }}>
                <span className="sr-only">동작</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {list.map((p) => (
              <tr key={p.id} className="hover-row">
                <td className="mono muted" style={{ fontSize: 12 }}>
                  {formatShortDate(p.studiedAt)}
                </td>
                <td>
                  <Tag tone={colorOf(colorIdx.get(p.topicId) ?? 0)}>{name.get(p.topicId)}</Tag>
                </td>
                <td className="num mono" style={{ color: 'var(--text)' }}>
                  {p.minutes}
                </td>
                <td className={`${s.note} ${s.hideMobile}`}>{p.notes}</td>
                <td style={{ textAlign: 'right' }}>
                  <RowActions label={`${p.studiedAt} 기록`} onEdit={() => onEdit(p)} onDelete={() => setDel(p)} />
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
      <ConfirmDialog
        open={!!del}
        title="공부 기록 삭제"
        message={del && `${del.studiedAt} ${del.minutes}분 기록을 지울까요?`}
        busy={remove.isPending}
        onClose={() => setDel(null)}
        onConfirm={() => del && remove.mutate(del.id, { onSuccess: () => setDel(null) })}
      />
    </section>
  )
}

// ---- 주간 통계 (서버 배치 결과) ----

function WeeklyStatsTable({ topics, colorIdx }: { topics: StudyTopic[]; colorIdx: Map<number, number> }) {
  const results = useQueries({
    queries: topics.map((t) => {
      const q = { topicId: t.id, size: 8, sort: 'weekStart,desc' }
      return { queryKey: listKey(studyWeeklyStats.path, q), queryFn: () => studyWeeklyStats.list(q) }
    }),
  })
  const rows: StudyTopicWeeklyStat[] = sortBy(
    results.flatMap((r) => r.data?.content ?? []),
    (r) => r.weekStart,
    'desc',
  )
  const name = new Map(topics.map((t) => [t.id, t.name]))
  const loading = results.some((r) => r.isLoading)
  return (
    <section className={s.box}>
      <div className={s.boxHead}>
        <h2>주제별 주간 통계</h2>
        <Link to="/settings" style={{ fontSize: 12 }}>
          다시 계산
        </Link>
      </div>
      <div className={rows.length ? undefined : s.pad}>
        <QueryState
          loading={loading}
          error={results.find((r) => r.error)?.error}
          empty={rows.length === 0}
          emptyView={<EmptyState compact title="주간 통계가 아직 없어요 — 설정에서 계산해 보세요" />}
        >
          <Table>
            <thead>
              <tr>
                <th scope="col">주 시작</th>
                <th scope="col">주제</th>
                <th scope="col" className="num">
                  세션
                </th>
                <th scope="col" className="num">
                  시간
                </th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id}>
                  <td className="mono">{r.weekStart}</td>
                  <td>
                    <Tag tone={colorOf(colorIdx.get(r.topicId) ?? 0)}>{name.get(r.topicId)}</Tag>
                  </td>
                  <td className="num mono">{r.sessionCount}</td>
                  <td className="num mono">{formatMinutes(r.totalMinutes)}</td>
                </tr>
              ))}
            </tbody>
          </Table>
        </QueryState>
      </div>
    </section>
  )
}

// ---- 오른쪽: 빠른 기록 + 이번 주 주제별 ----

const PRESETS = [25, 50, 90]

function QuickForm({
  topics,
  today,
  selectRef,
  autoFocus,
}: {
  topics: StudyTopic[]
  today: LocalDate
  selectRef: React.RefObject<HTMLSelectElement>
  autoFocus: boolean
}) {
  const [d, setD] = useState({ topicId: '', studiedAt: today, minutes: '50', notes: '' })
  const [errors, setErrors] = useState<Errors>({})
  const create = useCreate(studyProgresses)
  const toast = useToast()
  const topicId = d.topicId || String(topics[0]?.id ?? '')
  const hasTopics = topics.length > 0
  // 빠른 기록(?new=1)으로 들어오면 주제 목록이 도착한 뒤 첫 칸에 포커스
  useEffect(() => {
    if (autoFocus && hasTopics) selectRef.current?.focus()
  }, [autoFocus, hasTopics, selectRef])
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = {
      topicId: required(topicId, '주제'),
      studiedAt: required(d.studiedAt, '날짜'),
      minutes: required(d.minutes, '시간') ?? numRange(d.minutes, 1, 1440, true),
      notes: maxLen(d.notes, 500),
    }
    setErrors(errs)
    if (hasErrors(errs)) return
    create.mutate(
      { topicId: Number(topicId), studiedAt: d.studiedAt, minutes: Number(d.minutes), notes: optStr(d.notes) },
      {
        onSuccess: () => {
          toast.success('공부 기록을 저장했어요', `${d.minutes}분`)
          setD({ ...d, notes: '' })
        },
      },
    )
  }
  return (
    <form
      aria-label="공부 기록 추가"
      onSubmit={submit}
      noValidate
      className={s.box}
      style={{ padding: '12px 14px', display: 'flex', flexDirection: 'column', gap: 10 }}
    >
      <h2 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text)' }}>빠른 기록</h2>
      <Field label="주제" required error={errors.topicId}>
        <Select
          ref={selectRef}
          value={topicId}
          onChange={(e) => setD({ ...d, topicId: e.target.value })}
          disabled={!topics.length}
        >
          {topics.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name}
            </option>
          ))}
        </Select>
      </Field>
      <div className={s.two}>
        <Field label="날짜" required error={errors.studiedAt}>
          <Input type="date" mono value={d.studiedAt} onChange={(e) => setD({ ...d, studiedAt: e.target.value })} />
        </Field>
        <Field label="분" required error={errors.minutes}>
          <Input mono inputMode="numeric" value={d.minutes} onChange={(e) => setD({ ...d, minutes: e.target.value })} />
        </Field>
      </div>
      <div className={s.presets} role="group" aria-label="시간 빠른 선택">
        {PRESETS.map((m) => (
          <button
            key={m}
            type="button"
            className={s.preset}
            aria-pressed={d.minutes === String(m)}
            onClick={() => setD({ ...d, minutes: String(m) })}
          >
            {m}분
          </button>
        ))}
      </div>
      <Field label="메모" error={errors.notes}>
        <Textarea
          rows={2}
          value={d.notes}
          onChange={(e) => setD({ ...d, notes: e.target.value })}
          style={{ minHeight: 52 }}
        />
      </Field>
      <FormError error={create.error} />
      <Button
        type="submit"
        variant="primary"
        disabled={create.isPending || !topics.length}
        style={{ height: 30, fontSize: 13 }}
      >
        {create.isPending ? '저장 중…' : '기록 저장'}
      </Button>
    </form>
  )
}

function WeekSplit({
  topics,
  byTopic,
  today,
}: {
  topics: StudyTopic[]
  byTopic: Map<number, StudyProgress[]>
  today: LocalDate
}) {
  const from = weekStartOf(today)
  const rows = sortBy(
    topics.map((t, i) => {
      const mins = withinDates(byTopic.get(t.id) ?? [], (p) => p.studiedAt, from, shiftDate(from, 6)).reduce(
        (a, p) => a + p.minutes,
        0,
      )
      return { key: String(t.id), label: t.name, value: mins, display: formatMinutes(mins), color: colorOf(i) }
    }),
    (r) => r.value,
    'desc',
  )
  return (
    <div className={s.box} style={{ padding: '12px 14px', display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div style={{ display: 'flex', alignItems: 'center' }}>
        <h2 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text)' }}>이번 주 주제별</h2>
        <span className="mono muted" style={{ marginLeft: 'auto', fontSize: 11.5 }}>
          {formatShortDate(from)} 주
        </span>
      </div>
      {rows.length === 0 ? <EmptyState compact title="주제가 없어요" /> : <HBarList rows={rows} />}
    </div>
  )
}

// ---- 모달 ----

function TopicModal({ topic, onClose }: { topic?: StudyTopic; onClose: () => void }) {
  const [name, setName] = useState(topic?.name ?? '')
  const [description, setDescription] = useState(topic?.description ?? '')
  const [errors, setErrors] = useState<Errors>({})
  const [confirm, setConfirm] = useState(false)
  const create = useCreate(studyTopics)
  const update = useUpdate(studyTopics)
  const remove = useRemove(studyTopics)
  const m = topic ? update : create
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = { name: required(name, '주제 이름') ?? maxLen(name, 100), description: maxLen(description, 500) }
    setErrors(errs)
    if (hasErrors(errs)) return
    const body = { name: name.trim(), description: optStr(description) }
    if (topic) update.mutate({ id: topic.id, body }, { onSuccess: onClose })
    else create.mutate(body, { onSuccess: onClose })
  }
  return (
    <Modal
      open
      onClose={onClose}
      title={topic ? '주제 수정' : '주제 추가'}
      footer={
        <>
          {topic && (
            <Button variant="danger" style={{ marginRight: 'auto' }} onClick={() => setConfirm(true)}>
              삭제
            </Button>
          )}
          <Button onClick={onClose}>취소</Button>
          <Button variant="primary" type="submit" form="topic-form" disabled={m.isPending}>
            저장
          </Button>
        </>
      }
    >
      <form id="topic-form" onSubmit={submit} noValidate style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <Field label="이름" required error={errors.name}>
          <Input value={name} onChange={(e) => setName(e.target.value)} />
        </Field>
        <Field label="설명" error={errors.description}>
          <Input
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="JPA · 트랜잭션 · Spring Batch"
          />
        </Field>
        <FormError error={m.error ?? remove.error} />
      </form>
      <ConfirmDialog
        open={confirm}
        title="주제 삭제"
        message="기록이 남아 있으면 서버가 거부할 수 있어요. 지울까요?"
        busy={remove.isPending}
        onClose={() => setConfirm(false)}
        onConfirm={() => topic && remove.mutate(topic.id, { onSuccess: onClose, onError: () => setConfirm(false) })}
      />
    </Modal>
  )
}

function ProgressModal({ log, topics, onClose }: { log: StudyProgress; topics: StudyTopic[]; onClose: () => void }) {
  const [d, setD] = useState({
    topicId: String(log.topicId),
    studiedAt: log.studiedAt,
    minutes: String(log.minutes),
    notes: log.notes ?? '',
  })
  const [errors, setErrors] = useState<Errors>({})
  const update = useUpdate(studyProgresses)
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = {
      studiedAt: required(d.studiedAt, '날짜'),
      minutes: required(d.minutes, '시간') ?? numRange(d.minutes, 1, 1440, true),
      notes: maxLen(d.notes, 500),
    }
    setErrors(errs)
    if (hasErrors(errs)) return
    update.mutate(
      {
        id: log.id,
        body: {
          topicId: Number(d.topicId),
          studiedAt: d.studiedAt,
          minutes: Number(d.minutes),
          notes: optStr(d.notes),
        },
      },
      { onSuccess: onClose },
    )
  }
  return (
    <Modal
      open
      onClose={onClose}
      title="공부 기록 수정"
      footer={
        <>
          <Button onClick={onClose}>취소</Button>
          <Button variant="primary" type="submit" form="progress-form" disabled={update.isPending}>
            저장
          </Button>
        </>
      }
    >
      <form
        id="progress-form"
        onSubmit={submit}
        noValidate
        style={{ display: 'flex', flexDirection: 'column', gap: 10 }}
      >
        <Field label="주제" required>
          <Select value={d.topicId} onChange={(e) => setD({ ...d, topicId: e.target.value })}>
            {topics.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name}
              </option>
            ))}
          </Select>
        </Field>
        <div className={s.two}>
          <Field label="날짜" required error={errors.studiedAt}>
            <Input type="date" mono value={d.studiedAt} onChange={(e) => setD({ ...d, studiedAt: e.target.value })} />
          </Field>
          <Field label="분 (1–1440)" required error={errors.minutes}>
            <Input
              mono
              inputMode="numeric"
              value={d.minutes}
              onChange={(e) => setD({ ...d, minutes: e.target.value })}
            />
          </Field>
        </div>
        <Field label="메모" error={errors.notes}>
          <Textarea rows={2} value={d.notes} onChange={(e) => setD({ ...d, notes: e.target.value })} />
        </Field>
        <FormError error={update.error} />
      </form>
    </Modal>
  )
}
