import { MoreHorizontal, Plus } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { semesters } from '../api/pknu'
import { useCreate, useRemove, useUpdate } from '../api/resource'
import type { Course, Semester } from '../api/types'
import { PageContent, PageHeader } from '../components/layout/PageHeader'
import {
  Button,
  ConfirmDialog,
  EmptyState,
  Field,
  FormError,
  IconButton,
  Input,
  Modal,
  QueryState,
  Table,
  Tag,
  toneFor,
} from '../components/ui'
import { NotionLink } from '../components/ui/NotionLink'
import { usePeekTo } from '../components/layout/peek'
import { courseCategoryStore } from '../config/prefs'
import { useAllCourses, useSemesterBundle } from '../hooks/usePknu'
import { useToday } from '../hooks/useToday'
import { type LocalDate } from '../lib/date'
import { semesterStatus, weekNumber } from '../lib/select/pknu'
import { sortBy } from '../lib/select/range'
import { formatGpa, gpaOf, gradeTone } from '../lib/grade'
import { useStore } from '../lib/storage'
import { hasErrors, maxLen, required, type Errors } from '../lib/validate'
import s from './pknu/Pknu.module.css'
import { CourseModal } from './pknu/CourseModal'

type Dialog = { kind: 'semester'; semester?: Semester } | { kind: 'course'; semesterId: number; course?: Course } | null

/** PKNU: 학기 탭 없이 최신 학기가 위로 오게 쌓아서 스크롤 (이슈 #165). 과목은 누르면 작은 창 */
export default function PknuPage() {
  const today = useToday()
  const [params] = useSearchParams()
  const [dialog, setDialog] = useState<Dialog>(null)

  const all = useSemesterBundle()
  const courses = useAllCourses()
  const newestFirst = sortBy(all.semesters, (x) => x.startDate, 'desc')
  const total = gpaOf(courses.courses)

  // 사이드바·과목 창에서 ?semester=<id>로 들어오면 그 학기로 스크롤
  const target = params.get('semester')
  useEffect(() => {
    if (target) document.getElementById(`sem-${target}`)?.scrollIntoView({ block: 'start' })
  }, [target, all.semesters.length])

  return (
    <>
      <PageHeader title="PKNU">
        <span className={s.totalLine}>
          전체 평점 <b>{formatGpa(total.gpa)}</b> / 4.5 · 이수 <b>{total.earnedCredits}</b>학점
        </span>
        <Button variant="primary" icon={<Plus size={14} />} onClick={() => setDialog({ kind: 'semester' })}>
          학기 추가
        </Button>
      </PageHeader>

      <PageContent className={s.narrow}>
        <QueryState
          loading={all.isLoading && all.semesters.length === 0}
          error={all.error}
          onRetry={all.refetch}
          empty={all.semesters.length === 0}
          emptyView={
            <EmptyState
              title="등록된 학기가 없어요"
              description="학기를 만들고 과목을 추가해 보세요."
              action={
                <Button variant="primary" onClick={() => setDialog({ kind: 'semester' })}>
                  학기 추가
                </Button>
              }
            />
          }
        >
          {newestFirst.map((sem) => (
            <SemesterBlock
              key={sem.id}
              semester={sem}
              courseList={courses.courses.filter((c) => c.semesterId === sem.id)}
              loading={courses.isLoading}
              today={today}
              onEdit={() => setDialog({ kind: 'semester', semester: sem })}
              onAddCourse={() => setDialog({ kind: 'course', semesterId: sem.id })}
            />
          ))}
        </QueryState>
      </PageContent>

      {dialog?.kind === 'semester' && (
        <SemesterModal
          semester={dialog.semester}
          onClose={() => setDialog(null)}
          onSaved={(x) => setTimeout(() => document.getElementById(`sem-${x.id}`)?.scrollIntoView(), 300)}
        />
      )}
      {dialog?.kind === 'course' && (
        <CourseModal
          semesterList={newestFirst}
          semesterId={dialog.semesterId}
          course={dialog.course}
          onClose={() => setDialog(null)}
        />
      )}
    </>
  )
}

// ---- 학기 한 덩어리 ----

function SemesterBlock({
  semester,
  courseList,
  loading,
  today,
  onEdit,
  onAddCourse,
}: {
  semester: Semester
  courseList: Course[]
  loading: boolean
  today: LocalDate
  onEdit: () => void
  onAddCourse: () => void
}) {
  const semGpa = gpaOf(courseList)
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
    <article id={`sem-${semester.id}`} className={s.semBlock} aria-label={`${semester.name} 학기`}>
      <section className={s.semHead}>
        <h1>{semester.name}</h1>
        <Tag size="lg" tone={status === '진행 중' ? 'neutral' : status === '예정' ? 'blue' : 'gray'}>
          {status}
        </Tag>
        <span className={s.period}>
          {semester.startDate.replaceAll('-', '.')} → {semester.endDate.replaceAll('-', '.')}
          {wk && ` · ${wk}주차`}
        </span>
        <IconButton label={`${semester.name} 학기 수정`} size="sm" onClick={onEdit}>
          <MoreHorizontal size={15} />
        </IconButton>
      </section>
      <CoursesTable courseList={courseList} loading={loading} error={null} onAdd={onAddCourse} />
      {/* 학점·평점은 표 아래 (이슈 #172) */}
      <div className={`${s.credits} ${s.semTotals}`}>
        {[...byCat.entries()].map(([cat, n]) => (
          <span key={cat}>
            {cat} <b>{n}</b>
          </span>
        ))}
        <span>
          합계 <b>{total}</b>학점
        </span>
        <span>
          학기 평점 <b>{formatGpa(semGpa.gpa)}</b>
        </span>
      </div>
    </article>
  )
}

// ---- 과목 표 ----

function CoursesTable({
  courseList,
  loading,
  error,
  onAdd,
}: {
  courseList: Course[]
  loading: boolean
  error: unknown
  onAdd: () => void
}) {
  const categories = useStore(courseCategoryStore)
  const navigate = useNavigate()
  const peekTo = usePeekTo()
  const ordered = courseList.map((c, i) => ({ c, i }))
  return (
    <section className={s.box}>
      <div className={s.boxHead}>
        <h2>과목</h2>
        <Tag mono>{courseList.length}</Tag>
        <Button size="sm" icon={<Plus size={13} />} style={{ marginLeft: 'auto' }} onClick={onAdd}>
          과목 추가
        </Button>
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
              <th scope="col" className="num" style={{ width: 72 }}>
                성적
              </th>
            </tr>
          </thead>
          <tbody>
            {ordered.map(({ c, i }) => (
              <tr key={c.id} className={s.courseRow} onClick={() => navigate(peekTo('course', c.id))}>
                <td>
                  {/* 행 어디를 눌러도 과목 대시보드로 (이슈 #134) */}
                  <Link to={peekTo('course', c.id)} className={s.courseName} onClick={(e) => e.stopPropagation()}>
                    {c.name}
                  </Link>{' '}
                  {c.notionUrl && <NotionLink url={c.notionUrl} label={`${c.name} 노션 필기`} />}
                  {categories[c.id] && <Tag tone={toneFor(categories[c.id])}>{categories[c.id]}</Tag>}
                  <span className="sr-only">색 {i + 1}</span>
                </td>
                <td className={`${s.hideMobile} muted`}>{c.professor ?? '—'}</td>
                <td className="num mono">{c.credit}</td>
                <td style={{ textAlign: 'right' }}>
                  {c.grade ? (
                    <Tag mono tone={gradeTone(c.grade)}>
                      {c.grade}
                    </Tag>
                  ) : (
                    <span className="muted">—</span>
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

// ---- 모달 ----

export function SemesterModal({
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
