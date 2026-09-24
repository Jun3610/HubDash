import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil, X } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { courseBody, courses } from '../api/pknu'
import { useUpdate } from '../api/resource'
import type { Course, Grade } from '../api/types'
import {
  Button,
  Card,
  EmptyState,
  FormError,
  Peek,
  QueryState,
  SectionHeader,
  Tag,
  MarkdownEditor,
  toneFor,
  useToast,
} from '../components/ui'
import { NotionLink } from '../components/ui/NotionLink'
import { courseCategoryStore } from '../config/prefs'
import { useAllCourses } from '../hooks/usePknu'
import { useToday } from '../hooks/useToday'
import { formatGpa, GRADE_POINT, GRADES, gpaOf, gradeTone } from '../lib/grade'
import { semesterStatus } from '../lib/select/pknu'
import { joinTags, splitTags } from '../lib/format'
import { useStore } from '../lib/storage'
import { CourseModal } from './pknu/CourseModal'
import s from './pknu/Pknu.module.css'

const MEMO_MAX = 5000

function useCourse(courseId: number) {
  const q = useQuery({
    queryKey: [courses.path, 'one', courseId],
    queryFn: () => courses.get(courseId),
    enabled: Number.isFinite(courseId),
  })
  const all = useAllCourses()
  const course = q.data
  const semester = all.semesters.find((x) => x.id === course?.semesterId)
  return { q, all, course, semester }
}

type CourseData = ReturnType<typeof useCourse>

/** 예전 과목 페이지 주소는 학기 화면 위에 작은 창으로 */
export function CourseRedirect() {
  const { id } = useParams()
  return <Navigate to={`/pknu?course=${id}`} replace />
}

/** 과목 대시보드 (이슈 #134): 사이드바·과목 표에서 누르면 작은 창으로 뜬다 (이슈 #148) */
export function CoursePeek({ courseId, onClose }: { courseId: number; onClose: () => void }) {
  const data = useCourse(courseId)
  const { course, semester } = data
  const [editing, setEditing] = useState(false)

  return (
    <>
      <Peek
        label={course ? `${course.name} 과목` : '과목'}
        onClose={onClose}
        actions={
          course && (
            <Button size="sm" icon={<Pencil size={13} />} onClick={() => setEditing(true)}>
              과목 수정
            </Button>
          )
        }
      >
        <CourseContent data={data} />
      </Peek>
      {editing && course && semester && (
        <CourseModal
          semesterList={data.all.semesters}
          semesterId={semester.id}
          course={course}
          onClose={() => setEditing(false)}
          onDeleted={onClose}
        />
      )}
    </>
  )
}

function CourseContent({ data }: { data: CourseData }) {
  const { q, all, course, semester } = data
  const todayStr = useToday()
  return (
    <QueryState
      loading={q.isLoading}
      error={q.error}
      onRetry={() => void q.refetch()}
      empty={!course}
      emptyView={<EmptyState title="과목을 찾을 수 없어요" action={<Link to="/pknu">PKNU로</Link>} />}
    >
      {course && (
        <CourseDashboard
          key={course.id}
          course={course}
          semesterName={semester?.name}
          semesterLive={semester ? semesterStatus(semester.startDate, semester.endDate, todayStr) : undefined}
          sameSemester={all.courses.filter((c) => c.semesterId === course.semesterId)}
          allCourses={all.courses}
        />
      )}
    </QueryState>
  )
}

function CourseDashboard({
  course,
  semesterName,
  semesterLive,
  sameSemester,
  allCourses,
}: {
  course: Course
  semesterName?: string
  semesterLive?: string
  sameSemester: Course[]
  allCourses: Course[]
}) {
  const categories = useStore(courseCategoryStore)
  const update = useUpdate(courses)
  const toast = useToast()
  const qc = useQueryClient()
  const [memo, setMemo] = useState(course.memo ?? '')
  // 저장할 게 있는지는 서버에서 다시 받은 값이 아니라 '이 편집기가 마지막으로 보낸 값'과 비교한다 (이슈 #170).
  // 서버 값과 비교하면, 바로 닫았다 다시 열었을 때 늦게 도착한 새 값 때문에 예전 편집 내용이 다시 저장돼 덮어썼다
  const [saved, setSaved] = useState(course.memo ?? '')
  const dirty = memo !== saved

  const save = (patch: { grade?: Grade | null; memo?: string | null }, okText: string) =>
    update.mutate({ id: course.id, body: courseBody(course, patch) }, { onSuccess: () => toast.success(okText) })

  // 과목 메모는 자동 저장 (이슈 #158): 입력이 멈추고 0.8초 뒤, 그리고 창을 닫을 때 남은 내용을 바로.
  // 예전엔 저장 버튼 / ⌘S로만 보내서 Esc로 닫으면 적은 내용이 사라졌다
  const latest = useRef({ memo, dirty, course })
  useEffect(() => {
    latest.current = { memo, dirty, course }
  })
  const sentMemo = useRef<string | null>(null)
  const saveMemo = (text: string, base: Course) => {
    if (text.length > MEMO_MAX || text === sentMemo.current) return
    sentMemo.current = text
    const value = text.trim() ? text : null
    const before = saved
    setSaved(text)
    // 창을 닫고 바로 다시 열어도 새 내용으로 시작하게 캐시를 먼저 바꿔 둔다
    qc.setQueryData<Course>([courses.path, 'one', base.id], (old) => (old ? { ...old, memo: value } : old))
    update.mutate(
      { id: base.id, body: courseBody(base, { memo: value }) },
      {
        onError: () => {
          sentMemo.current = null
          setSaved(before)
          void qc.invalidateQueries({ queryKey: [courses.path, 'one', base.id] })
        },
      },
    )
  }
  useEffect(() => {
    if (!dirty || update.isPending) return
    const t = setTimeout(() => saveMemo(memo, course), 800)
    return () => clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [memo, dirty, update.isPending])
  useEffect(
    () => () => {
      const l = latest.current
      if (l.dirty) saveMemo(l.memo, l.course)
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  )

  const sem = gpaOf(sameSemester)
  const total = gpaOf(allCourses)
  const cat = categories[course.id]

  return (
    <>
      <section className={s.semHead}>
        <h1>{course.name}</h1>
        {cat && (
          <Tag size="lg" tone={toneFor(cat)}>
            {cat}
          </Tag>
        )}
        {course.notionUrl && <NotionLink url={course.notionUrl} label={`${course.name} 노션 필기`} />}
        <CourseTags course={course} />
        <div className={s.credits}>
          <span>
            학기 평점 <b>{formatGpa(sem.gpa)}</b>
          </span>
          <span>
            전체 평점 <b>{formatGpa(total.gpa)}</b> / 4.5
          </span>
        </div>
      </section>

      <div className={s.courseInfo}>
        <Card>
          <span className={s.infoLabel}>학점</span>
          <span className={s.infoValue}>{course.credit}</span>
        </Card>
        <Card>
          <span className={s.infoLabel}>학기</span>
          <Link to={`/pknu?semester=${course.semesterId}`} className={s.infoValue}>
            {semesterName ?? '—'}
          </Link>
          {semesterLive && <span className={s.infoSub}>{semesterLive}</span>}
        </Card>
        <Card>
          <span className={s.infoLabel}>성적</span>
          <span className={s.infoValue}>
            {course.grade ? (
              <>
                {course.grade} <small>{GRADE_POINT[course.grade].toFixed(1)}</small>
              </>
            ) : (
              '미정'
            )}
          </span>
        </Card>
        <Card>
          <span className={s.infoLabel}>교수</span>
          <span className={s.infoValue} style={{ fontSize: 16 }}>
            {course.professor ?? '—'}
          </span>
        </Card>
      </div>

      <Card>
        <SectionHeader title="성적" meta="누르면 바로 저장 · 4.5 만점" />
        <div className={s.gradePick} role="radiogroup" aria-label="성적">
          {GRADES.map((g) => (
            <button
              key={g}
              type="button"
              role="radio"
              aria-checked={course.grade === g}
              data-tone={gradeTone(g)}
              disabled={update.isPending}
              onClick={() => course.grade !== g && save({ grade: g }, `성적을 ${g}로 저장했어요`)}
            >
              {g}
              <small>{GRADE_POINT[g].toFixed(1)}</small>
            </button>
          ))}
          <button
            type="button"
            role="radio"
            aria-checked={!course.grade}
            disabled={update.isPending}
            onClick={() => course.grade && save({ grade: null }, '성적을 비웠어요')}
          >
            미정
          </button>
        </div>
        <span className="muted" style={{ fontSize: 12 }}>
          {semesterName} 성적 나온 {sem.gradedCredits}학점 / 전체 {sem.totalCredits}학점 · 이수 {total.earnedCredits}
          학점
        </span>
      </Card>

      <Card className={s.memoCard}>
        <SectionHeader
          title="과목 메모"
          meta={
            memo.length > MEMO_MAX
              ? `${MEMO_MAX.toLocaleString()}자까지만 저장돼요`
              : update.isPending
                ? '저장 중…'
                : dirty
                  ? '곧 자동 저장'
                  : saved
                    ? '저장됨'
                    : '적으면 자동 저장'
          }
        />
        <MarkdownEditor
          label="과목 메모"
          value={course.memo ?? ''}
          onChange={setMemo}
          placeholder="시험 범위, 과제 방식 같은 걸 적어 두세요 — # 제목, - 목록, [] 할 일"
          className={s.memoEditor}
        />
        <span className={s.memoCount} data-over={memo.length > MEMO_MAX}>
          {memo.length.toLocaleString()} / {MEMO_MAX.toLocaleString()}
        </span>
        <FormError error={update.error} />
      </Card>
    </>
  )
}

/** 과목 태그: 누르면 바로 저장, 색은 강조색 (이슈 #175) */
function CourseTags({ course }: { course: Course }) {
  const update = useUpdate(courses)
  const qc = useQueryClient()
  const tags = splitTags(course.tags)
  const [adding, setAdding] = useState(false)
  const [draft, setDraft] = useState('')
  const saveTags = (next: string[]) => {
    const value = joinTags(next) || null
    // 메모 저장과 같은 과목을 고치므로 캐시를 먼저 바꿔 두고 보낸다
    qc.setQueryData<Course>([courses.path, 'one', course.id], (old) => (old ? { ...old, tags: value } : old))
    update.mutate({ id: course.id, body: courseBody({ ...course, tags: value }) })
  }
  const add = () => {
    const t = draft.trim().replace(/,/g, '')
    if (t && !tags.includes(t) && joinTags([...tags, t]).length <= 300) saveTags([...tags, t])
    setDraft('')
    setAdding(false)
  }
  return (
    <span className={s.courseTags}>
      {tags.map((t) => (
        <span key={t} className={s.courseTag}>
          {t}
          <button type="button" aria-label={`${t} 태그 빼기`} onClick={() => saveTags(tags.filter((x) => x !== t))}>
            <X size={11} />
          </button>
        </span>
      ))}
      {adding ? (
        <input
          className={s.courseTagInput}
          aria-label="새 태그"
          autoFocus
          maxLength={30}
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onBlur={add}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault()
              add()
            } else if (e.key === 'Escape') {
              // 창까지 닫히지 않게 입력만 취소
              e.stopPropagation()
              e.nativeEvent.stopImmediatePropagation()
              setDraft('')
              setAdding(false)
            }
          }}
        />
      ) : (
        <button type="button" className={s.courseTagAdd} onClick={() => setAdding(true)}>
          + 태그
        </button>
      )}
    </span>
  )
}
