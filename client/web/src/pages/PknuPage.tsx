import { useQueries } from '@tanstack/react-query'
import { CheckCircle2, Circle, MoreHorizontal, Plus } from 'lucide-react'
import { NotionLink } from '../components/ui/NotionLink'
import { useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { assignmentWeeklyStats, useRunWeeklyStat } from '../api/analytics'
import { assignments, courses, semesters } from '../api/pknu'
import { listKey, useCreate, useRemove, useUpdate } from '../api/resource'
import type { Assignment, AssignmentWeeklyStat, Course, Semester } from '../api/types'
import { PageContent, PageHeader } from '../components/layout/PageHeader'
import {
  Button,
  Checkbox,
  ConfirmDialog,
  DdayBadge,
  EmptyState,
  Field,
  FormError,
  IconButton,
  Input,
  Modal,
  ProgressBar,
  QueryState,
  Select,
  Table,
  Tabs,
  Tag,
  Textarea,
  toneFor,
  useToast,
  type Tone,
} from '../components/ui'
import { courseCategoryStore } from '../config/prefs'
import { useOptimistic } from '../hooks/useOptimistic'
import { pickCurrentSemester, useSemesterBundle } from '../hooks/usePknu'
import { useToday } from '../hooks/useToday'
import { datePart, formatShortDate, shiftDate, weekdayKo, weekStartOf, type LocalDate } from '../lib/date'
import { courseProgress, mergeWeekly, semesterStatus, weekNumber } from '../lib/select/pknu'
import { sortBy } from '../lib/select/range'
import { useStore } from '../lib/storage'
import { hasErrors, isUrl, maxLen, numRange, optStr, required, type Errors } from '../lib/validate'
import s from './pknu/Pknu.module.css'

/** 과목 순서대로 사이드바 색 점과 같은 계열의 라벨 색 */
const COURSE_TONES: Tone[] = ['blue', 'green', 'purple', 'yellow', 'orange', 'neutral']

type Dialog =
  | { kind: 'semester'; semester?: Semester }
  | { kind: 'course'; course?: Course }
  | { kind: 'assignment'; assignment?: Assignment }
  | null

export default function PknuPage() {
  const today = useToday()
  const [params, setParams] = useSearchParams()
  const [dialog, setDialog] = useState<Dialog>(null)

  const all = useSemesterBundle()
  const sorted = sortBy(all.semesters, (x) => x.startDate)
  const selectedId = Number(params.get('semester')) || pickCurrentSemester(all.semesters, today)?.id || null
  const bundle = useSemesterBundle(selectedId)
  const semester = bundle.semester

  const select = (id: string) =>
    setParams(
      (p) => {
        const n = new URLSearchParams(p)
        n.set('semester', id)
        return n
      },
      { replace: true },
    )

  return (
    <>
      <PageHeader
        title="학업 · PKNU"
        tabs={
          sorted.length > 0 ? (
            <Tabs
              inHeader
              label="학기"
              value={String(selectedId ?? '')}
              onChange={(k) => (k === 'new' ? setDialog({ kind: 'semester' }) : select(k))}
              items={[
                ...sorted.map((x) => ({
                  key: String(x.id),
                  label: x.name.replace(/\s*학기$/, ''),
                  count: x.id === selectedId ? bundle.courses.length : undefined,
                })),
                { key: 'new', label: '+ 학기' },
              ]}
            />
          ) : undefined
        }
      >
        <Button disabled={!semester} onClick={() => setDialog({ kind: 'course' })}>
          과목 추가
        </Button>
        <Button
          variant="primary"
          icon={<Plus size={14} />}
          disabled={bundle.courses.length === 0}
          title={bundle.courses.length === 0 ? '과목을 먼저 추가하세요' : undefined}
          onClick={() => setDialog({ kind: 'assignment' })}
        >
          과제 추가
        </Button>
      </PageHeader>

      <PageContent>
        <QueryState
          loading={all.isLoading && all.semesters.length === 0}
          error={all.error}
          onRetry={all.refetch}
          empty={all.semesters.length === 0}
          emptyView={
            <EmptyState
              title="등록된 학기가 없어요"
              description="학기를 만들고 과목과 과제를 추가해 보세요."
              action={
                <Button variant="primary" onClick={() => setDialog({ kind: 'semester' })}>
                  학기 추가
                </Button>
              }
            />
          }
        >
          {semester && (
            <>
              <SemesterSummary
                semester={semester}
                courseList={bundle.courses}
                today={today}
                onEdit={() => setDialog({ kind: 'semester', semester })}
              />
              <div className={s.grid}>
                <div className={s.col}>
                  <CoursesTable
                    courseList={bundle.courses}
                    byCourse={bundle.assignmentsByCourse}
                    loading={bundle.isLoading}
                    error={bundle.error}
                    today={today}
                    onAdd={() => setDialog({ kind: 'course' })}
                    onEdit={(course) => setDialog({ kind: 'course', course })}
                  />
                  <WeeklyCompletion courseList={bundle.courses} />
                </div>
                <AssignmentList
                  list={bundle.assignments}
                  courseList={bundle.courses}
                  loading={bundle.isLoading}
                  today={today}
                  onEdit={(assignment) => setDialog({ kind: 'assignment', assignment })}
                  onAdd={() => setDialog({ kind: 'assignment' })}
                />
              </div>
            </>
          )}
        </QueryState>
      </PageContent>

      {dialog?.kind === 'semester' && (
        <SemesterModal
          semester={dialog.semester}
          onClose={() => setDialog(null)}
          onSaved={(x) => select(String(x.id))}
        />
      )}
      {dialog?.kind === 'course' && semester && (
        <CourseModal
          semesterList={sorted}
          semesterId={semester.id}
          course={dialog.course}
          onClose={() => setDialog(null)}
        />
      )}
      {dialog?.kind === 'assignment' && (
        <AssignmentModal
          courseList={bundle.courses}
          today={today}
          assignment={dialog.assignment}
          onClose={() => setDialog(null)}
        />
      )}
    </>
  )
}

// ---- 학기 요약 ----

function SemesterSummary({
  semester,
  courseList,
  today,
  onEdit,
}: {
  semester: Semester
  courseList: Course[]
  today: LocalDate
  onEdit: () => void
}) {
  const categories = useStore(courseCategoryStore)
  const status = semesterStatus(semester.startDate, semester.endDate, today)
  const wk = weekNumber(semester.startDate, semester.endDate, today)
  const total = courseList.reduce((a, c) => a + c.credit, 0)
  const byCat = new Map<string, number>()
  for (const c of courseList) {
    const cat = categories[c.id]
    if (cat) byCat.set(cat, (byCat.get(cat) ?? 0) + c.credit)
  }
  return (
    <section className={s.semHead}>
      <h1>{semester.name}</h1>
      <Tag size="lg" tone={status === '진행 중' ? 'neutral' : status === '예정' ? 'blue' : 'gray'}>
        {status}
      </Tag>
      <span className={s.period}>
        {semester.startDate.replaceAll('-', '.')} → {semester.endDate.replaceAll('-', '.')}
        {wk && ` · ${wk}주차`}
      </span>
      <IconButton label="학기 수정" size="sm" onClick={onEdit}>
        <MoreHorizontal size={15} />
      </IconButton>
      <div className={s.credits}>
        {[...byCat.entries()].map(([cat, n]) => (
          <span key={cat}>
            {cat} <b>{n}</b>
          </span>
        ))}
        <span>
          합계 <b>{total}</b>학점
        </span>
      </div>
    </section>
  )
}

// ---- 과목 표 ----

function CoursesTable({
  courseList,
  byCourse,
  loading,
  error,
  today,
  onAdd,
  onEdit,
}: {
  courseList: Course[]
  byCourse: Map<number, Assignment[]>
  loading: boolean
  error: unknown
  today: LocalDate
  onAdd: () => void
  onEdit: (c: Course) => void
}) {
  const categories = useStore(courseCategoryStore)
  const rows = courseList.map((c, i) => ({ c, i, p: courseProgress(byCourse.get(c.id) ?? [], today) }))
  // 다음 마감이 가까운 과목 먼저, 마감 없는 과목은 뒤로
  const ordered = sortBy(rows, (r) => r.p.next?.dueDate ?? '9999')
  return (
    <section className={s.box}>
      <div className={s.boxHead}>
        <h2>과목</h2>
        <Tag mono>{courseList.length}</Tag>
        <span className={s.right}>다음 마감 순</span>
      </div>
      {loading && courseList.length === 0 ? (
        <div style={{ padding: '8px 14px' }}>
          <QueryState loading error={null}>
            {null}
          </QueryState>
        </div>
      ) : error ? (
        <div style={{ padding: '0 14px' }}>
          <QueryState loading={false} error={error}>
            {null}
          </QueryState>
        </div>
      ) : courseList.length === 0 ? (
        <EmptyState
          title="과목이 없어요"
          action={
            <Button variant="primary" onClick={onAdd}>
              과목 추가
            </Button>
          }
        />
      ) : (
        <Table>
          <thead>
            <tr>
              <th scope="col">과목</th>
              <th scope="col" className={s.hideMobile} style={{ width: 120 }}>
                교수
              </th>
              <th scope="col" className="num" style={{ width: 48 }}>
                학점
              </th>
              <th scope="col" style={{ width: 170 }}>
                과제
              </th>
              <th scope="col" className="num" style={{ width: 80 }}>
                다음 마감
              </th>
            </tr>
          </thead>
          <tbody>
            {ordered.map(({ c, i, p }) => (
              <tr key={c.id}>
                <td>
                  <button type="button" className={s.courseName} onClick={() => onEdit(c)}>
                    {c.name}
                  </button>{' '}
                  {c.notionUrl && <NotionLink url={c.notionUrl} label={`${c.name} 노션 필기`} />}
                  {categories[c.id] && <Tag tone={toneFor(categories[c.id])}>{categories[c.id]}</Tag>}
                  <span className="sr-only">색 {i + 1}</span>
                </td>
                <td className={`${s.hideMobile} muted`}>{c.professor ?? '—'}</td>
                <td className="num mono">{c.credit}</td>
                <td>
                  <div className={s.progress}>
                    <ProgressBar
                      value={p.total ? (p.done / p.total) * 100 : 0}
                      color="green"
                      label={`${c.name} 과제 진행`}
                    />
                    <span className={s.ratio}>
                      {p.done}/{p.total}
                    </span>
                  </div>
                </td>
                <td style={{ textAlign: 'right' }}>
                  {p.next ? (
                    <DdayBadge due={p.next.dueDate} today={today} />
                  ) : (
                    <Tag mono tone="gray">
                      —
                    </Tag>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
    </section>
  )
}

// ---- 주간 과제 완료율 ----

function WeeklyCompletion({ courseList }: { courseList: Course[] }) {
  const today = useToday()
  const toast = useToast()
  const run = useRunWeeklyStat(assignmentWeeklyStats)
  const results = useQueries({
    queries: courseList.map((c) => {
      const q = { courseId: c.id, size: 20, sort: 'weekStart,desc' }
      return { queryKey: listKey(assignmentWeeklyStats.path, q), queryFn: () => assignmentWeeklyStats.list(q) }
    }),
  })
  const loading = results.some((r) => r.isLoading)
  const error = results.find((r) => r.error)?.error
  const rows: AssignmentWeeklyStat[] = results.flatMap((r) => r.data?.content ?? [])
  const weeks = mergeWeekly(rows, 4)
  const lastWeek = shiftDate(weekStartOf(today), -7)
  return (
    <section className={s.box} style={{ padding: '12px 14px', display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <h2 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text)' }}>주간 과제 완료율</h2>
        <span className={`muted ${s.hideMobile}`} style={{ fontSize: 12 }}>
          assignment-weekly-stats
        </span>
        <Button
          size="sm"
          style={{ marginLeft: 'auto' }}
          disabled={run.isPending}
          title={`${lastWeek} 주를 다시 계산`}
          onClick={() =>
            run.mutate(lastWeek, {
              onSuccess: (r) =>
                toast.success(`통계를 다시 계산했어요`, `#${r.jobExecutionId} ${r.status} · ${r.weekStart} 주`),
            })
          }
        >
          {run.isPending ? '계산 중…' : '통계 다시 계산'}
        </Button>
      </div>
      <QueryState
        loading={loading}
        error={error}
        empty={weeks.length === 0}
        emptyView={<EmptyState compact title="아직 주간 통계가 없어요 — 다시 계산을 눌러 보세요" />}
      >
        <div className={s.weeks}>
          {weeks.map((w) => {
            const pct = Math.round(w.rate * 100)
            return (
              <div key={w.weekStart} className={s.week}>
                <span className={s.weekLabel}>{formatShortDate(w.weekStart)} 주</span>
                <div className={s.weekValue}>
                  <b>{pct}</b>
                  <span>%</span>
                  <span>
                    {w.done}/{w.total}
                  </span>
                </div>
                <ProgressBar value={pct} color={pct >= 70 ? 'green' : 'yellow'} thin />
              </div>
            )
          })}
        </div>
      </QueryState>
    </section>
  )
}

// ---- 과제 목록 (GitHub Issues 모양) ----

function AssignmentList({
  list,
  courseList,
  loading,
  today,
  onEdit,
  onAdd,
}: {
  list: Assignment[]
  courseList: Course[]
  loading: boolean
  today: LocalDate
  onEdit: (a: Assignment) => void
  onAdd: () => void
}) {
  const [show, setShow] = useState<'open' | 'closed'>('open')
  const update = useUpdate(assignments)
  const opt = useOptimistic<number>()
  const courseIdx = new Map(courseList.map((c, i) => [c.id, i]))
  const courseName = new Map(courseList.map((c) => [c.id, c.name]))
  const isDone = (a: Assignment) => opt.value(a.id, a.completed)
  const open = sortBy(
    list.filter((a) => !isDone(a)),
    (a) => a.dueDate,
  )
  const closed = sortBy(
    list.filter((a) => isDone(a)),
    (a) => a.updatedAt,
    'desc',
  )
  const shown = show === 'open' ? open : closed

  const toggle = (a: Assignment) => {
    const next = !isDone(a)
    opt.set(a.id, next)
    update.mutate(
      { id: a.id, body: { courseId: a.courseId, title: a.title, dueDate: a.dueDate, completed: next, notes: a.notes } },
      { onSettled: () => opt.clear(a.id) },
    )
  }

  return (
    <section className={s.box} aria-label="과제 목록">
      <div className={s.issueTabs}>
        <button type="button" className={s.issueTab} aria-pressed={show === 'open'} onClick={() => setShow('open')}>
          <Circle size={15} color="var(--green)" strokeWidth={2} />
          {open.length} 진행 중
        </button>
        <button type="button" className={s.issueTab} aria-pressed={show === 'closed'} onClick={() => setShow('closed')}>
          <CheckCircle2 size={15} color="var(--purple)" strokeWidth={2} />
          {closed.length} 완료
        </button>
        <span className={`muted`} style={{ marginLeft: 'auto', fontSize: 12 }}>
          {show === 'open' ? '마감순' : '최근 완료순'}
        </span>
      </div>
      <div className={s.issueList}>
        {loading && list.length === 0 ? (
          <div style={{ padding: '8px 14px' }}>
            <QueryState loading error={null}>
              {null}
            </QueryState>
          </div>
        ) : shown.length === 0 ? (
          <EmptyState
            title={show === 'open' ? '진행 중인 과제가 없어요' : '완료한 과제가 없어요'}
            action={
              show === 'open' && courseList.length > 0 ? (
                <Button variant="primary" onClick={onAdd}>
                  과제 추가
                </Button>
              ) : undefined
            }
          />
        ) : (
          shown.map((a) => {
            const done = isDone(a)
            const idx = courseIdx.get(a.courseId) ?? 0
            return (
              <div key={a.id} className={s.issue}>
                <button
                  type="button"
                  className={s.state}
                  aria-label={done ? `${a.title} 다시 열기` : `${a.title} 완료로 표시`}
                  onClick={() => toggle(a)}
                >
                  {done ? (
                    <CheckCircle2 size={16} color="var(--purple)" strokeWidth={2} />
                  ) : (
                    <Circle size={16} color="var(--green)" strokeWidth={2} />
                  )}
                </button>
                <div className={s.issueBody}>
                  <div className={s.issueTitle}>
                    <button type="button" onClick={() => onEdit(a)}>
                      {a.title}
                    </button>
                    <Tag tone={COURSE_TONES[idx % COURSE_TONES.length]}>{courseName.get(a.courseId)}</Tag>
                  </div>
                  <span className={s.meta}>
                    {done
                      ? `${formatShortDate(datePart(a.updatedAt))} 완료`
                      : `${formatShortDate(a.dueDate)} (${weekdayKo(a.dueDate)}) 마감${a.notes ? ' · 메모 있음' : ''}`}
                  </span>
                </div>
                <DdayBadge due={a.dueDate} today={today} completed={done} />
              </div>
            )
          })
        )}
      </div>
    </section>
  )
}

// ---- 모달 ----

function SemesterModal({
  semester,
  onClose,
  onSaved,
}: {
  semester?: Semester
  onClose: () => void
  onSaved: (x: Semester) => void
}) {
  const [d, setD] = useState({
    name: semester?.name ?? '',
    startDate: semester?.startDate ?? '',
    endDate: semester?.endDate ?? '',
  })
  const [errors, setErrors] = useState<Errors<keyof typeof d>>({})
  const [confirm, setConfirm] = useState(false)
  const create = useCreate(semesters)
  const update = useUpdate(semesters)
  const remove = useRemove(semesters)
  const m = semester ? update : create
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = {
      name: required(d.name, '학기 이름') ?? maxLen(d.name, 100),
      startDate: required(d.startDate, '시작일'),
      endDate:
        required(d.endDate, '종료일') ??
        (d.startDate && d.endDate < d.startDate ? '종료일이 시작일보다 빨라요' : undefined),
    }
    setErrors(errs)
    if (hasErrors(errs)) return
    const body = { name: d.name.trim(), startDate: d.startDate, endDate: d.endDate }
    const done = (x: Semester) => {
      onSaved(x)
      onClose()
    }
    if (semester) update.mutate({ id: semester.id, body }, { onSuccess: done })
    else create.mutate(body, { onSuccess: done })
  }
  const set = (k: keyof typeof d) => (e: React.ChangeEvent<HTMLInputElement>) => setD({ ...d, [k]: e.target.value })
  return (
    <Modal
      open
      onClose={onClose}
      title={semester ? '학기 수정' : '학기 추가'}
      footer={
        <>
          {semester && (
            <Button variant="danger" style={{ marginRight: 'auto' }} onClick={() => setConfirm(true)}>
              삭제
            </Button>
          )}
          <Button onClick={onClose}>취소</Button>
          <Button variant="primary" type="submit" form="semester-form" disabled={m.isPending}>
            저장
          </Button>
        </>
      }
    >
      <form id="semester-form" className={s.formGrid} onSubmit={submit} noValidate>
        <Field label="이름" required error={errors.name} className={s.full}>
          <Input placeholder="2026-2 학기" value={d.name} onChange={set('name')} />
        </Field>
        <Field label="시작일" required error={errors.startDate}>
          <Input type="date" mono value={d.startDate} onChange={set('startDate')} />
        </Field>
        <Field label="종료일" required error={errors.endDate}>
          <Input type="date" mono value={d.endDate} onChange={set('endDate')} />
        </Field>
        <div className={s.full}>
          <FormError error={m.error ?? remove.error} />
        </div>
      </form>
      <ConfirmDialog
        open={confirm}
        title="학기 삭제"
        message="과목이 남아 있으면 서버가 거부할 수 있어요. 지울까요?"
        busy={remove.isPending}
        onClose={() => setConfirm(false)}
        onConfirm={() =>
          semester && remove.mutate(semester.id, { onSuccess: onClose, onError: () => setConfirm(false) })
        }
      />
    </Modal>
  )
}

function CourseModal({
  semesterList,
  semesterId,
  course,
  onClose,
}: {
  semesterList: Semester[]
  semesterId: number
  course?: Course
  onClose: () => void
}) {
  const categories = useStore(courseCategoryStore)
  const [d, setD] = useState({
    semesterId: String(course?.semesterId ?? semesterId),
    name: course?.name ?? '',
    professor: course?.professor ?? '',
    credit: String(course?.credit ?? 3),
    category: course ? (categories[course.id] ?? '') : '',
    notionUrl: course?.notionUrl ?? '',
  })
  const [errors, setErrors] = useState<Errors<keyof typeof d>>({})
  const [confirm, setConfirm] = useState(false)
  const create = useCreate(courses)
  const update = useUpdate(courses)
  const remove = useRemove(courses)
  const m = course ? update : create
  const knownCats = [...new Set(Object.values(categories))]
  const saveCategory = (id: number) =>
    courseCategoryStore.set((all) => {
      const next = { ...all }
      if (d.category.trim()) next[id] = d.category.trim()
      else delete next[id]
      return next
    })
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = {
      name: required(d.name, '과목 이름') ?? maxLen(d.name, 100),
      professor: maxLen(d.professor, 100),
      credit: required(d.credit, '학점') ?? numRange(d.credit, 1, 6, true),
      category: maxLen(d.category, 10),
      notionUrl: d.notionUrl.trim() ? (isUrl(d.notionUrl.trim()) ?? maxLen(d.notionUrl, 1000)) : undefined,
    }
    setErrors(errs)
    if (hasErrors(errs)) return
    const body = {
      semesterId: Number(d.semesterId),
      name: d.name.trim(),
      professor: optStr(d.professor),
      credit: Number(d.credit),
      // 수정할 때 빠뜨리면 서버가 기존 노션 링크를 지우므로 항상 보낸다
      notionUrl: optStr(d.notionUrl),
    }
    const done = (x: Course) => {
      saveCategory(x.id)
      onClose()
    }
    if (course) update.mutate({ id: course.id, body }, { onSuccess: done })
    else create.mutate(body, { onSuccess: done })
  }
  const set = (k: keyof typeof d) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setD({ ...d, [k]: e.target.value })
  return (
    <Modal
      open
      onClose={onClose}
      title={course ? '과목 수정' : '과목 추가'}
      footer={
        <>
          {course && (
            <Button variant="danger" style={{ marginRight: 'auto' }} onClick={() => setConfirm(true)}>
              삭제
            </Button>
          )}
          <Button onClick={onClose}>취소</Button>
          <Button variant="primary" type="submit" form="course-form" disabled={m.isPending}>
            저장
          </Button>
        </>
      }
    >
      <form id="course-form" className={s.formGrid} onSubmit={submit} noValidate>
        <Field label="과목 이름" required error={errors.name} className={s.full}>
          <Input value={d.name} onChange={set('name')} />
        </Field>
        <Field label="교수">
          <Input value={d.professor} onChange={set('professor')} />
        </Field>
        <Field label="학점 (1–6)" required error={errors.credit}>
          <Input mono inputMode="numeric" value={d.credit} onChange={set('credit')} />
        </Field>
        <Field label="학기" required>
          <Select value={d.semesterId} onChange={set('semesterId')}>
            {semesterList.map((x) => (
              <option key={x.id} value={x.id}>
                {x.name}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="분류" hint="예: 시경, 컴공, 교양 · 이 브라우저에 저장" error={errors.category}>
          <Input list="course-cats" value={d.category} onChange={set('category')} />
        </Field>
        <Field label="노션 필기 페이지" error={errors.notionUrl} className={s.full}>
          <Input
            mono
            type="url"
            placeholder="https://www.notion.so/…"
            value={d.notionUrl}
            onChange={set('notionUrl')}
          />
        </Field>
        <datalist id="course-cats">
          {knownCats.map((c) => (
            <option key={c} value={c} />
          ))}
        </datalist>
        <div className={s.full}>
          <FormError error={m.error ?? remove.error} />
        </div>
      </form>
      <ConfirmDialog
        open={confirm}
        title="과목 삭제"
        message="과제가 남아 있으면 서버가 거부할 수 있어요. 지울까요?"
        busy={remove.isPending}
        onClose={() => setConfirm(false)}
        onConfirm={() => course && remove.mutate(course.id, { onSuccess: onClose, onError: () => setConfirm(false) })}
      />
    </Modal>
  )
}

function AssignmentModal({
  courseList,
  today,
  assignment,
  onClose,
}: {
  courseList: Course[]
  today: LocalDate
  assignment?: Assignment
  onClose: () => void
}) {
  const [d, setD] = useState({
    courseId: String(assignment?.courseId ?? courseList[0]?.id ?? ''),
    title: assignment?.title ?? '',
    dueDate: assignment?.dueDate ?? shiftDate(today, 7),
    completed: assignment?.completed ?? false,
    notes: assignment?.notes ?? '',
  })
  const [errors, setErrors] = useState<Errors>({})
  const [confirm, setConfirm] = useState(false)
  const create = useCreate(assignments)
  const update = useUpdate(assignments)
  const remove = useRemove(assignments)
  const m = assignment ? update : create
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = {
      courseId: required(d.courseId, '과목'),
      title: required(d.title, '제목') ?? maxLen(d.title, 200),
      dueDate: required(d.dueDate, '마감일'),
      notes: maxLen(d.notes, 1000),
    }
    setErrors(errs)
    if (hasErrors(errs)) return
    const body = {
      courseId: Number(d.courseId),
      title: d.title.trim(),
      dueDate: d.dueDate,
      completed: d.completed,
      notes: optStr(d.notes),
    }
    if (assignment) update.mutate({ id: assignment.id, body }, { onSuccess: onClose })
    else create.mutate(body, { onSuccess: onClose })
  }
  return (
    <Modal
      open
      onClose={onClose}
      title={assignment ? '과제 수정' : '과제 추가'}
      footer={
        <>
          {assignment && (
            <Button variant="danger" style={{ marginRight: 'auto' }} onClick={() => setConfirm(true)}>
              삭제
            </Button>
          )}
          <Button onClick={onClose}>취소</Button>
          <Button variant="primary" type="submit" form="assignment-form" disabled={m.isPending}>
            저장
          </Button>
        </>
      }
    >
      <form id="assignment-form" className={s.formGrid} onSubmit={submit} noValidate>
        <Field label="제목" required error={errors.title} className={s.full}>
          <Input value={d.title} onChange={(e) => setD({ ...d, title: e.target.value })} />
        </Field>
        <Field label="과목" required error={errors.courseId}>
          <Select value={d.courseId} onChange={(e) => setD({ ...d, courseId: e.target.value })}>
            {courseList.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="마감일" required error={errors.dueDate}>
          <Input type="date" mono value={d.dueDate} onChange={(e) => setD({ ...d, dueDate: e.target.value })} />
        </Field>
        <Field label="메모" error={errors.notes} className={s.full}>
          <Textarea rows={3} value={d.notes} onChange={(e) => setD({ ...d, notes: e.target.value })} />
        </Field>
        <div className={s.full}>
          <Checkbox checked={d.completed} onChange={(v) => setD({ ...d, completed: v })} label="완료" />
        </div>
        <div className={s.full}>
          <FormError error={m.error ?? remove.error} />
        </div>
      </form>
      <ConfirmDialog
        open={confirm}
        title="과제 삭제"
        message={`"${assignment?.title}"을(를) 지울까요?`}
        busy={remove.isPending}
        onClose={() => setConfirm(false)}
        onConfirm={() => assignment && remove.mutate(assignment.id, { onSuccess: onClose })}
      />
    </Modal>
  )
}
