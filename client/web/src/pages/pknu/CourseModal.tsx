import { useState, type FormEvent } from 'react'
import { courseBody, courses } from '../../api/pknu'
import { useCreate, useRemove, useUpdate } from '../../api/resource'
import type { Course, Semester } from '../../api/types'
import { Button, ConfirmDialog, Field, FormError, Input, Modal, Select } from '../../components/ui'
import { courseCategoryStore } from '../../config/prefs'
import { useStore } from '../../lib/storage'
import { hasErrors, isUrl, maxLen, numRange, optStr, required, type Errors } from '../../lib/validate'
import s from './Pknu.module.css'

export function CourseModal({
  semesterList,
  semesterId,
  course,
  onClose,
  onDeleted,
}: {
  semesterList: Semester[]
  semesterId: number
  course?: Course
  onClose: () => void
  onDeleted?: () => void
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
    const fields = {
      semesterId: Number(d.semesterId),
      name: d.name.trim(),
      professor: optStr(d.professor),
      credit: Number(d.credit),
      notionUrl: optStr(d.notionUrl),
    }
    // 수정은 기존 성적·메모를 그대로 담아 보낸다 (빠뜨리면 서버가 지움)
    const body = course ? courseBody(course, fields) : { ...fields, grade: null, memo: null }
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
        onConfirm={() =>
          course &&
          remove.mutate(course.id, {
            onSuccess: () => {
              onClose()
              onDeleted?.()
            },
            onError: () => setConfirm(false),
          })
        }
      />
    </Modal>
  )
}
