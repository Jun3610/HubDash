import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, buildQuery, connectionStatus, request, unwrap } from './client'
import { apiUrl } from '../config/connection'

describe('unwrap (봉투 해제)', () => {
  it('success:true면 data를 꺼낸다', () => {
    expect(unwrap(200, { success: true, data: { id: 1 }, errorCode: null, message: null })).toEqual({ id: 1 })
  })

  it('success:false면 errorCode와 message를 그대로 담은 server 오류', () => {
    const body = { success: false, data: null, errorCode: 'INVALID_REQUEST', message: 'must not be blank' }
    try {
      unwrap(400, body)
      expect.unreachable()
    } catch (e) {
      expect(e).toBeInstanceOf(ApiError)
      const err = e as ApiError
      expect(err.kind).toBe('server')
      expect(err.errorCode).toBe('INVALID_REQUEST')
      expect(err.message).toBe('must not be blank')
      expect(err.status).toBe(400)
    }
  })

  it('401은 본문과 상관없이 unauthorized', () => {
    expect(() => unwrap(401, { success: false, errorCode: 'UNAUTHORIZED', message: 'invalid api key' })).toThrow(
      expect.objectContaining({ kind: 'unauthorized' }),
    )
  })

  it('봉투가 아닌 5xx(프록시가 서버에 못 붙음)는 network', () => {
    expect(() => unwrap(502, '')).toThrow(expect.objectContaining({ kind: 'network' }))
  })

  it('봉투가 아닌 4xx는 invalid', () => {
    expect(() => unwrap(404, '<html>')).toThrow(expect.objectContaining({ kind: 'invalid' }))
  })

  it('서버 500 봉투는 server 오류로 message를 보여준다', () => {
    const body = {
      success: false,
      data: null,
      errorCode: 'INTERNAL_SERVER_ERROR',
      message: '서버 내부 오류가 발생했습니다',
    }
    expect(() => unwrap(500, body)).toThrow(
      expect.objectContaining({ kind: 'server', errorCode: 'INTERNAL_SERVER_ERROR' }),
    )
  })
})

describe('buildQuery', () => {
  it('빈 값은 뺀다', () => {
    expect(buildQuery({ page: 0, size: 20, sort: 'createdAt,desc', courseId: undefined, q: '' })).toBe(
      '?page=0&size=20&sort=createdAt%2Cdesc',
    )
  })
  it('없으면 빈 문자열', () => {
    expect(buildQuery()).toBe('')
  })
})

describe('apiUrl', () => {
  it('기본 주소면 프록시용 상대 경로', () => {
    expect(apiUrl('/api/memo/memos', 'http://localhost:8080')).toBe('/api/memo/memos')
    expect(apiUrl('/api/memo/memos', 'http://localhost:8080/')).toBe('/api/memo/memos')
  })
  it('다른 주소면 절대 경로', () => {
    expect(apiUrl('/api/memo/memos', 'http://192.168.0.10:8080/')).toBe('http://192.168.0.10:8080/api/memo/memos')
  })
})

describe('request', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('X-API-KEY 헤더를 붙이고 연결 상태를 online으로 바꾼다', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        new Response(JSON.stringify({ success: true, data: [1], errorCode: null, message: null }), { status: 200 }),
      )
    vi.stubGlobal('fetch', fetchMock)
    await expect(request('/api/x')).resolves.toEqual([1])
    const [, init] = fetchMock.mock.calls[0]
    expect(init.headers['X-API-KEY']).toBe('dev-local-key')
    expect(connectionStatus.get()).toBe('online')
  })

  it('fetch 자체가 실패하면 network 오류 + offline', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    await expect(request('/api/x')).rejects.toMatchObject({ kind: 'network' })
    expect(connectionStatus.get()).toBe('offline')
  })

  it('401이면 unauthorized 상태', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{"success":false}', { status: 401 })))
    await expect(request('/api/x')).rejects.toMatchObject({ kind: 'unauthorized' })
    expect(connectionStatus.get()).toBe('unauthorized')
  })

  it('DELETE의 204 빈 본문은 성공', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })))
    await expect(request('/api/x', { method: 'DELETE' })).resolves.toBeUndefined()
  })
})
