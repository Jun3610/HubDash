import { MoreHorizontal, Plus } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { courses, semesters } from '../api/pknu'
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
  Select,
  Table,
  Tabs,
  Tag,
  toneFor,
} from '../components/ui'
import { NotionLink } from '../components/ui/NotionLink'
import { courseCategoryStore } from '../config/prefs'
import { pickCurrentSemester, useSemesterBundle } from '../hooks/usePknu'
import { useToday } from '../hooks/useToday'
import { type LocalDate } from '../lib/date'
import { semesterStatus, weekNumber } from '../lib/select/pknu'
import { sortBy } from '../lib/select/range'
import { useStore } from '../lib/storage'
import { hasErrors, isUrl, maxLen, numRange, optStr, required, type Errors } from '../lib/validate'
import s from './pknu/Pknu.module.css'

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
                courseList={bundle.courses}
                today={today}
                onEdit={() => setDialog({ kind: 'semester', semester })}
              />
              <CoursesTable
                courseList={bundle.courses}
                loading={bundle.isLoading}
                error={bundle.error}
                onAdd={() => setDialog({ kind: 'course' })}
                onEdit={(course) => setDialog({ kind: 'course', course })}
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
  loading,
  error,
  onAdd,
  onEdit,
}: {
  courseList: Course[]
  loading: boolean
  error: unknown
  onAdd: () => void
  onEdit: (c: Course) => void
}) {
  const categories = useStore(courseCategoryStore)
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
            </tr>
          </thead>
          <tbody>
            {ordered.map(({ c, i }) => (
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
      // 수정할 때 빠뜨리면 서버가 기존 값을 지우므로 항상 보낸다
      notionUrl: optStr(d.notionUrl),
      grade: course?.grade ?? null,
      memo: course?.memo ?? null,
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
        message="이 과목을 지울까요? 성적과 메모도 함께 지워져요."
        busy={remove.isPending}
        onClose={() => setConfirm(false)}
        onConfirm={() => course && remove.mutate(course.id, { onSuccess: onClose, onError: () => setConfirm(false) })}
      />
    </Modal>
  )
}
