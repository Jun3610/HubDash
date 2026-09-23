import { apiUrl, connectionStore } from '../config/connection'
import { createMemoryStore } from '../lib/storage'

export interface Envelope<T> {
  success: boolean
  data: T | null
  errorCode: string | null
  message: string | null
}

export type ApiErrorKind =
  | 'unauthorized' // 401 — API 키 확인
  | 'network' // 서버 꺼짐, 연결 실패
  | 'server' // success:false (INVALID_REQUEST, NOT_FOUND …)
  | 'invalid' // 봉투 형태가 아님 (프록시 오류 페이지 등)

export class ApiError extends Error {
  readonly kind: ApiErrorKind
  readonly status?: number
  readonly errorCode?: string

  constructor(kind: ApiErrorKind, message: string, opts: { status?: number; errorCode?: string } = {}) {
    super(message)
    this.name = 'ApiError'
    this.kind = kind
    this.status = opts.status
    this.errorCode = opts.errorCode
  }
}

// ---- 연결 상태 (배너, 사이드바 상태 점) ----

export type ConnectionStatus = 'unknown' | 'online' | 'offline' | 'unauthorized'

export const connectionStatus = createMemoryStore<ConnectionStatus>('unknown')

function report(status: ConnectionStatus) {
  if (connectionStatus.get() !== status) connectionStatus.set(status)
}

// ---- 응답 해석 ----

function isEnvelope(body: unknown): body is Envelope<unknown> {
  return typeof body === 'object' && body !== null && 'success' in body
}

/** HTTP 상태와 본문을 받아 data를 꺼내거나 ApiError로 바꾼다 (fetch와 분리해 테스트 가능) */
export function unwrap<T>(status: number, body: unknown): T {
  if (status === 401) {
    throw new ApiError('unauthorized', 'API 키를 확인해 주세요', { status, errorCode: 'UNAUTHORIZED' })
  }
  // DELETE는 204 No Content로 본문 없이 온다
  if (status >= 200 && status < 300 && (body === null || body === '')) return undefined as T
  if (!isEnvelope(body)) {
    // Vite 프록시는 서버가 꺼져 있으면 500/502/504를 본문 없이 돌려준다
    if (status >= 500) throw new ApiError('network', '서버에 연결할 수 없어요', { status })
    throw new ApiError('invalid', `예상하지 못한 응답입니다 (HTTP ${status})`, { status })
  }
  if (!body.success) {
    throw new ApiError('server', body.message ?? '요청을 처리하지 못했어요', {
      status,
      errorCode: body.errorCode ?? 'UNKNOWN',
    })
  }
  return body.data as T
}

export type Query = Record<string, string | number | boolean | undefined | null>

export function buildQuery(query?: Query): string {
  if (!query) return ''
  const params = new URLSearchParams()
  for (const [k, v] of Object.entries(query)) {
    if (v !== undefined && v !== null && v !== '') params.append(k, String(v))
  }
  const s = params.toString()
  return s ? `?${s}` : ''
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  query?: Query
  body?: unknown
  signal?: AbortSignal
  /** 설정 화면의 연결 테스트처럼 저장 전 값으로 호출할 때 */
  connection?: { baseUrl: string; apiKey: string }
}

export async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const conn = opts.connection ?? connectionStore.get()
  const url = apiUrl(path, conn.baseUrl) + buildQuery(opts.query)
  let res: Response
  try {
    res = await fetch(url, {
      method: opts.method ?? 'GET',
      headers: {
        'X-API-KEY': conn.apiKey,
        ...(opts.body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      },
      body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
      signal: opts.signal,
    })
  } catch (e) {
    if ((e as Error).name === 'AbortError') throw e
    if (!opts.connection) report('offline')
    throw new ApiError('network', '서버에 연결할 수 없어요')
  }

  let body: unknown = null
  const text = await res.text()
  if (text) {
    try {
      body = JSON.parse(text)
    } catch {
      body = text
    }
  }

  try {
    const data = unwrap<T>(res.status, body)
    if (!opts.connection) report('online')
    return data
  } catch (e) {
    if (!opts.connection && e instanceof ApiError) {
      if (e.kind === 'unauthorized') report('unauthorized')
      else if (e.kind === 'network') report('offline')
      else report('online')
    }
    throw e
  }
}

export const http = {
  get: <T>(path: string, query?: Query) => request<T>(path, { query }),
  post: <T>(path: string, body: unknown) => request<T>(path, { method: 'POST', body }),
  put: <T>(path: string, body: unknown) => request<T>(path, { method: 'PUT', body }),
  del: (path: string) => request<void>(path, { method: 'DELETE' }),
}
