import { http, HttpResponse, type HttpHandler } from 'msw'
import { buildSeed, nextId, type Row } from './seed'

// 서버 명세와 같은 모양(봉투, 페이지, 필수 쿼리 400)으로 응답하는 목 서버
const db = buildSeed()

const ok = (data: unknown, status = 200) =>
  HttpResponse.json({ success: true, data, errorCode: null, message: null }, { status })
const fail = (status: number, errorCode: string, message: string) =>
  HttpResponse.json({ success: false, data: null, errorCode, message }, { status })

const nowStamp = () => {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}.${String(d.getMilliseconds()).padStart(3, '0')}`
}

interface Spec {
  path: string
  /** 목록 필수 쿼리 */
  required?: string
  /** 필수 요청 필드 */
  fields: string[]
  validate?: (body: Record<string, unknown>) => string | null
  /** 응답 가공 (끼니의 items/totals 등) */
  view?: (row: Row) => Row
}

function totalsOf(items: Row[]) {
  const sum = (k: string) => items.reduce((a, it) => a + (Number(it[k]) || 0), 0)
  return {
    calories: sum('calories'),
    carbsG: sum('carbsG'),
    proteinG: sum('proteinG'),
    fatG: sum('fatG'),
    sodiumMg: sum('sodiumMg'),
  }
}

const mealView = (r: Row): Row => {
  const items = db['/api/health/meal-items'].filter((it) => it.mealRecordId === r.id)
  return { ...r, items, totals: totalsOf(items) }
}

const range = (k: string, min: number, max: number) => (b: Record<string, unknown>) =>
  b[k] != null && (Number(b[k]) < min || Number(b[k]) > max) ? `${k}: ${min}~${max} 사이여야 합니다` : null

const SPECS: Spec[] = [
  { path: '/api/pknu/semesters', fields: ['name', 'startDate', 'endDate'] },
  {
    path: '/api/pknu/courses',
    required: 'semesterId',
    fields: ['semesterId', 'name', 'credit'],
    validate: range('credit', 1, 6),
  },
  { path: '/api/pknu/assignments', required: 'courseId', fields: ['courseId', 'title', 'dueDate', 'completed'] },
  { path: '/api/study/topics', fields: ['name'] },
  {
    path: '/api/study/progresses',
    required: 'topicId',
    fields: ['topicId', 'studiedAt', 'minutes'],
    validate: range('minutes', 1, 1440),
  },
  { path: '/api/life/habits', fields: ['name'] },
  { path: '/api/life/habit-logs', required: 'habitId', fields: ['habitId', 'performedAt', 'completed'] },
  { path: '/api/life/reading-logs', fields: ['title', 'startedAt'], validate: range('rating', 1, 5) },
  { path: '/api/health/meal-records', fields: ['consumedAt', 'mealType'], view: mealView },
  { path: '/api/health/meal-items', required: 'mealRecordId', fields: ['mealRecordId', 'name', 'calories'] },
  { path: '/api/health/workout-logs', fields: ['performedAt', 'type', 'durationMinutes'] },
  { path: '/api/health/logs', fields: ['recordedAt'], validate: range('sleepHours', 0, 24) },
  {
    path: '/api/schedule/events',
    fields: ['title', 'startAt', 'endAt', 'allDay'],
    validate: (b) => (String(b.endAt) <= String(b.startAt) ? 'endAt는 startAt보다 이후여야 합니다' : null),
  },
  {
    path: '/api/memo/memos',
    fields: ['title', 'content'],
    validate: (b) =>
      String(b.content ?? '').length > 5000
        ? 'content는 5000자 이하여야 합니다'
        : String(b.tags ?? '').length > 300
          ? 'tags는 300자 이하여야 합니다'
          : null,
  },
  { path: '/api/hub/categories', fields: ['name'] },
  { path: '/api/hub/links', required: 'categoryId', fields: ['categoryId', 'title', 'url'] },
  { path: '/api/reminder/reminders', fields: ['title', 'targetAt', 'sent'] },
]

function page(rows: Row[], url: URL) {
  const pageNo = Number(url.searchParams.get('page') ?? 0)
  const size = Number(url.searchParams.get('size') ?? 20)
  const sort = url.searchParams.get('sort')
  let sorted = rows
  if (sort) {
    const [key, dir = 'asc'] = sort.split(',')
    const sign = dir.toLowerCase() === 'desc' ? -1 : 1
    sorted = [...rows].sort((a, b) => {
      const va = a[key] as string | number
      const vb = b[key] as string | number
      return va < vb ? -sign : va > vb ? sign : 0
    })
  }
  const content = sorted.slice(pageNo * size, pageNo * size + size)
  return { content, page: pageNo, size, totalElements: rows.length, totalPages: Math.ceil(rows.length / size) }
}

function validate(spec: Spec, body: Record<string, unknown>): string | null {
  const missing = spec.fields.filter((f) => body[f] === undefined || body[f] === null || body[f] === '')
  if (missing.length) return missing.map(() => 'must not be blank').join(', ')
  return spec.validate?.(body) ?? null
}

function crud(spec: Spec): HttpHandler[] {
  const table = () => db[spec.path]
  const view = spec.view ?? ((r: Row) => r)
  return [
    http.get(spec.path, ({ request }) => {
      const url = new URL(request.url)
      let rows = table()
      if (spec.required) {
        const v = url.searchParams.get(spec.required)
        if (!v) return fail(400, 'INVALID_REQUEST', `필수 파라미터가 누락되었습니다: ${spec.required}`)
        rows = rows.filter((r) => String(r[spec.required!]) === v)
      }
      return ok(page(rows.map(view), url))
    }),
    http.get(`${spec.path}/:id`, ({ params }) => {
      const r = table().find((x) => x.id === Number(params.id))
      return r ? ok(view(r)) : fail(404, 'NOT_FOUND', `찾을 수 없습니다: ${params.id}`)
    }),
    http.post(spec.path, async ({ request }) => {
      const body = (await request.json()) as Record<string, unknown>
      const err = validate(spec, body)
      if (err) return fail(400, 'INVALID_REQUEST', err)
      const at = nowStamp()
      const r = { ...body, id: nextId(), createdAt: at, updatedAt: at } as Row
      table().push(r)
      return ok(view(r), 201)
    }),
    http.put(`${spec.path}/:id`, async ({ params, request }) => {
      const body = (await request.json()) as Record<string, unknown>
      const i = table().findIndex((x) => x.id === Number(params.id))
      if (i < 0) return fail(404, 'NOT_FOUND', `찾을 수 없습니다: ${params.id}`)
      const err = validate(spec, body)
      if (err) return fail(400, 'INVALID_REQUEST', err)
      table()[i] = { ...table()[i], ...body, updatedAt: nowStamp() }
      return ok(view(table()[i]))
    }),
    http.delete(`${spec.path}/:id`, ({ params }) => {
      const i = table().findIndex((x) => x.id === Number(params.id))
      if (i < 0) return fail(404, 'NOT_FOUND', `찾을 수 없습니다: ${params.id}`)
      table().splice(i, 1)
      if (spec.path === '/api/health/meal-records') {
        db['/api/health/meal-items'] = db['/api/health/meal-items'].filter(
          (it) => it.mealRecordId !== Number(params.id),
        )
      }
      return new HttpResponse(null, { status: 204 })
    }),
  ]
}

function singleton(path: string, required: string): HttpHandler[] {
  return [
    http.get(path, () => ok(db[path][0])),
    http.put(path, async ({ request }) => {
      const body = (await request.json()) as Record<string, unknown>
      if (!body[required]) return fail(400, 'INVALID_REQUEST', 'must not be blank')
      db[path][0] = { ...db[path][0], ...body, updatedAt: nowStamp() }
      return ok(db[path][0])
    }),
  ]
}

const STATS: [string, string | null][] = [
  ['/api/health/analytics/meal-weekly-stats', null],
  ['/api/health/analytics/weekly-stats', null],
  ['/api/study/analytics/topic-weekly-stats', 'topicId'],
  ['/api/life/analytics/habit-weekly-stats', 'habitId'],
  ['/api/pknu/analytics/assignment-weekly-stats', 'courseId'],
]

let jobSeq = 40
function stats(path: string, required: string | null): HttpHandler[] {
  return [
    http.get(path, ({ request }) => {
      const url = new URL(request.url)
      let rows = db[path]
      if (required) {
        const v = url.searchParams.get(required)
        if (!v) return fail(400, 'INVALID_REQUEST', `필수 파라미터가 누락되었습니다: ${required}`)
        rows = rows.filter((r) => String(r[required]) === v)
      }
      return ok(page(rows, url))
    }),
    http.post(`${path}/batch-runs`, async ({ request }) => {
      const body = (await request.json()) as { weekStart?: string }
      if (!body.weekStart) return fail(400, 'INVALID_REQUEST', 'must not be null')
      return ok({ jobExecutionId: ++jobSeq, status: 'COMPLETED', weekStart: body.weekStart })
    }),
  ]
}

export const handlers: HttpHandler[] = [
  http.get('/api/health/meal-records/daily-summary', ({ request }) => {
    const date = new URL(request.url).searchParams.get('date')
    if (!date) return fail(400, 'INVALID_REQUEST', '필수 파라미터가 누락되었습니다: date')
    const recs = db['/api/health/meal-records'].filter((r) => String(r.consumedAt).startsWith(date)).map(mealView)
    const types = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK']
    // 서버와 같이 기록이 없는 끼니도 4종 모두 돌려준다
    const meals = types.map((t) => {
      const items = recs.filter((r) => r.mealType === t).flatMap((r) => r.items as Row[])
      return { mealType: t, itemCount: items.length, totals: totalsOf(items) }
    })
    return ok({ date, totals: totalsOf(recs.flatMap((r) => r.items as Row[])), meals })
  }),
  ...singleton('/api/user/profile', 'displayName'),
  ...singleton('/api/user/settings', 'theme'),
  ...singleton('/api/health/diet-goal', 'carbsRule'),
  ...STATS.flatMap(([p, r]) => stats(p, r)),
  ...SPECS.flatMap(crud),
]
