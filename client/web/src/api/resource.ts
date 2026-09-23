import { useMutation, useQueries, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { http, type Query } from './client'
import type { Id, Page } from './types'

/** 날짜 범위 필터가 없는 서버에서 "넉넉히" 가져올 때 쓰는 크기 */
export const BIG_PAGE = 200

export interface Resource<Res, Req> {
  path: string
  list(query?: Query): Promise<Page<Res>>
  get(id: Id): Promise<Res>
  create(body: Req): Promise<Res>
  update(id: Id, body: Req): Promise<Res>
  remove(id: Id): Promise<void>
}

export function defineResource<Res, Req>(path: string): Resource<Res, Req> {
  return {
    path,
    list: (query) => http.get<Page<Res>>(path, query),
    get: (id) => http.get<Res>(`${path}/${id}`),
    create: (body) => http.post<Res>(path, body),
    update: (id, body) => http.put<Res>(`${path}/${id}`, body),
    remove: (id) => http.del(`${path}/${id}`),
  }
}

/** "/api/health/meal-items" → "/api/health" */
export function domainOf(path: string): string {
  return path.split('/').slice(0, 3).join('/')
}

/**
 * 한 도메인의 데이터가 바뀌면 그 도메인의 모든 쿼리를 무효화한다.
 * (끼니 음식이 바뀌면 daily-summary도, 과제가 바뀌면 홈 KPI도 다시 받아야 하므로 도메인 단위가 안전하다)
 */
export function invalidateDomain(qc: QueryClient, path: string) {
  const domain = domainOf(path)
  return qc.invalidateQueries({ predicate: (q) => String(q.queryKey[0]).startsWith(domain) })
}

export function listKey(path: string, query?: Query) {
  return [path, 'list', query ?? {}] as const
}

export function useList<Res>(res: Resource<Res, unknown>, query?: Query, opts: { enabled?: boolean } = {}) {
  return useQuery({
    queryKey: listKey(res.path, query),
    queryFn: () => res.list(query),
    enabled: opts.enabled,
  })
}

/**
 * 부모별로만 조회되는 목록(과목별 과제, 주제별 기록 …)을 병렬로 받아 합친다.
 * 하나라도 불러오는 중이면 isLoading, 하나라도 실패하면 error를 채운다.
 */
export function useListsByParent<Res>(
  res: Resource<Res, unknown>,
  param: string,
  parentIds: Id[],
  extra: Query = { size: BIG_PAGE },
  opts: { enabled?: boolean } = {},
) {
  return useQueries({
    queries: parentIds.map((id) => {
      const query = { ...extra, [param]: id }
      return {
        queryKey: listKey(res.path, query),
        queryFn: () => res.list(query),
        enabled: opts.enabled,
      }
    }),
    combine: (results) => {
      const byParent = new Map<Id, Res[]>()
      results.forEach((r, i) => byParent.set(parentIds[i], r.data?.content ?? []))
      return {
        data: results.flatMap((r) => r.data?.content ?? []),
        byParent,
        isLoading: results.some((r) => r.isLoading),
        error: results.find((r) => r.error)?.error ?? null,
        refetch: () => Promise.all(results.map((r) => r.refetch())),
      }
    },
  })
}

export function useCreate<Res, Req>(res: Resource<Res, Req>) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: Req) => res.create(body),
    onSuccess: () => invalidateDomain(qc, res.path),
  })
}

export function useUpdate<Res, Req>(res: Resource<Res, Req>) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: Id; body: Req }) => res.update(id, body),
    onSuccess: () => invalidateDomain(qc, res.path),
  })
}

export function useRemove(res: Resource<unknown, unknown>) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: Id) => res.remove(id),
    onSuccess: () => invalidateDomain(qc, res.path),
  })
}
