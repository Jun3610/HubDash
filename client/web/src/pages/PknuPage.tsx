import { MoreHorizontal, Plus } from 'lucide-react'
import { useState, type FormEvent } from 'react'
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
  Tabs,
  Tag,
  toneFor,
} from '../components/ui'
import { NotionLink } from '../components/ui/NotionLink'
import { courseCategoryStore } from '../config/prefs'
import { pickCurrentSemester, useAllCourses, useSemesterBundle } from '../hooks/usePknu'
import { useToday } from '../hooks/useToday'
import { type LocalDate } from '../lib/date'
import { semesterStatus, weekNumber } from '../lib/select/pknu'
import { sortBy } from '../lib/select/range'
import { formatGpa, gpaOf, gradeTone } from '../lib/grade'
import { useStore } from '../lib/storage'
import { hasErrors, maxLen, required, type Errors } from '../lib/validate'
import s from './pknu/Pknu.module.css'
import { CourseModal } from './pknu/CourseModal'

type Dialog = { kind: 'semester'; semester?: Semester } | { kind: 'course'; course?: Course } | null

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
        <Button
          variant="primary"
          icon={<Plus size={14} />}
          disabled={!semester}
          onClick={() => setDialog({ kind: 'course' })}
        >
          과목 추가
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
              description="학기를 만들고 과목을 추가해 보세요."
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
                semesterList={sorted}
                onSelect={select}
                courseList={bundle.courses}
                today={today}
                onEdit={() => setDialog({ kind: 'semester', semester })}
              />
              <CoursesTable
                courseList={bundle.courses}
                loading={bundle.isLoading}
                error={bundle.error}
                onAdd={() => setDialog({ kind: 'course' })}
              />
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
    </>
  )
}

// ---- 학기 요약 ----

function SemesterSummary({
  semester,
  semesterList,
  courseList,
  today,
  onEdit,
  onSelect,
}: {
  semester: Semester
  semesterList: Semester[]
  courseList: Course[]
  today: LocalDate
  onEdit: () => void
  onSelect: (id: string) => void
}) {
  const allCourses = useAllCourses()
  const semGpa = gpaOf(courseList)
  const totalGpa = gpaOf(allCourses.courses)
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
    <>
      {/* 학기가 헤더의 작은 탭에만 있으면 잘 안 보여서 본문에도 크게 (이슈 #132) */}
      <div className={s.semSwitch} role="tablist" aria-label="학기 선택">
        {semesterList.map((x) => (
          <button
            key={x.id}
            type="button"
            role="tab"
            aria-selected={x.id === semester.id}
            onClick={() => onSelect(String(x.id))}
          >
            {x.name}
            <small>{semesterStatus(x.startDate, x.endDate, today)}</small>
          </button>
        ))}
      </div>
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
          <span>
            학기 평점 <b>{formatGpa(semGpa.gpa)}</b>
          </span>
          <span>
            전체 평점 <b>{formatGpa(totalGpa.gpa)}</b> / 4.5
          </span>
        </div>
      </section>
    </>
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
  const ordered = courseList.map((c, i) => ({ c, i }))
  return (
    <section className={s.box}>
      <div className={s.boxHead}>
        <h2>과목</h2>
        <Tag mono>{courseList.length}</Tag>
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
              <tr key={c.id} className={s.courseRow} onClick={() => navigate(`/pknu/courses/${c.id}`)}>
                <td>
                  {/* 행 어디를 눌러도 과목 대시보드로 (이슈 #134) */}
                  <Link to={`/pknu/courses/${c.id}`} className={s.courseName} onClick={(e) => e.stopPropagation()}>
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
