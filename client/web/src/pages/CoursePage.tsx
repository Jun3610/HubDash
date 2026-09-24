import { useQuery } from '@tanstack/react-query'
import { Pencil, Save } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { courseBody, courses } from '../api/pknu'
import { useUpdate } from '../api/resource'
import type { Course, Grade } from '../api/types'
import { PageContent, PageHeader } from '../components/layout/PageHeader'
import {
  Button,
  Card,
  EmptyState,
  FormError,
  QueryState,
  SectionHeader,
  Tag,
  Textarea,
  toneFor,
  useToast,
} from '../components/ui'
import { NotionLink } from '../components/ui/NotionLink'
import { courseCategoryStore } from '../config/prefs'
import { useAllCourses } from '../hooks/usePknu'
import { useToday } from '../hooks/useToday'
import { formatGpa, GRADE_POINT, GRADES, gpaOf, gradeTone } from '../lib/grade'
import { semesterStatus } from '../lib/select/pknu'
import { useStore } from '../lib/storage'
import { CourseModal } from './pknu/CourseModal'
import s from './pknu/Pknu.module.css'

const MEMO_MAX = 5000

/** 과목 대시보드: 학점 · 학기 · 성적과 과목 메모 (이슈 #134) */
export default function CoursePage() {
  const { id } = useParams()
  const courseId = Number(id)
  const navigate = useNavigate()
  const q = useQuery({
    queryKey: [courses.path, 'one', courseId],
    queryFn: () => courses.get(courseId),
    enabled: Number.isFinite(courseId),
  })
  const all = useAllCourses()
  const todayStr = useToday()
  const course = q.data
  const semester = all.semesters.find((x) => x.id === course?.semesterId)
  const [editing, setEditing] = useState(false)

  return (
    <>
      <PageHeader
        title="학업 · PKNU"
        titleTo={semester ? `/pknu?semester=${semester.id}` : '/pknu'}
        sub={course?.name ?? '과목'}
      >
        {course && (
          <Button icon={<Pencil size={13} />} onClick={() => setEditing(true)}>
            과목 수정
          </Button>
        )}
      </PageHeader>
      <PageContent>
        <QueryState
          loading={q.isLoading}
          error={q.error}
          onRetry={() => void q.refetch()}
          empty={!course}
          emptyView={<EmptyState title="과목을 찾을 수 없어요" action={<Link to="/pknu">학업으로</Link>} />}
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
      </PageContent>
      {editing && course && semester && (
        <CourseModal
          semesterList={all.semesters}
          semesterId={semester.id}
          course={course}
          onClose={() => setEditing(false)}
          onDeleted={() => navigate(`/pknu?semester=${semester.id}`)}
        />
      )}
    </>
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
  const [memo, setMemo] = useState(course.memo ?? '')
  const dirty = memo !== (course.memo ?? '')

  const save = (patch: { grade?: Grade | null; memo?: string | null }, okText: string) =>
    update.mutate({ id: course.id, body: courseBody(course, patch) }, { onSuccess: () => toast.success(okText) })

  const saveMemo = () => {
    if (!dirty || memo.length > MEMO_MAX) return
    save({ memo: memo.trim() ? memo : null }, '메모를 저장했어요')
  }

  // ⌘S / Ctrl+S로 메모 저장
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 's') {
        e.preventDefault()
        saveMemo()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  })

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

      <Card>
        <SectionHeader
          title="과목 메모"
          meta={dirty ? '저장 안 됨' : course.memo ? '저장됨' : undefined}
          actions={
            <Button
              size="sm"
              variant="primary"
              icon={<Save size={13} />}
              disabled={!dirty || update.isPending || memo.length > MEMO_MAX}
              onClick={saveMemo}
            >
              저장
            </Button>
          }
        />
        <Textarea
          aria-label="과목 메모"
          rows={10}
          placeholder="시험 범위, 과제 방식, 교수님 스타일 같은 걸 적어 두세요 (⌘S로 저장)"
          value={memo}
          onChange={(e) => setMemo(e.target.value)}
          style={{ minHeight: 200 }}
        />
        <span className={s.memoCount} data-over={memo.length > MEMO_MAX}>
          {memo.length.toLocaleString()} / {MEMO_MAX.toLocaleString()}
        </span>
        <FormError error={update.error} />
      </Card>
    </>
  )
}
