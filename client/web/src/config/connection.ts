import { createStore } from '../lib/storage'

export const DEFAULT_BASE_URL = 'http://localhost:8080'
export const DEFAULT_API_KEY = 'dev-local-key'

export interface Connection {
  baseUrl: string
  apiKey: string
}

export const connectionStore = createStore<Connection>('hubdash.connection', {
  baseUrl: DEFAULT_BASE_URL,
  apiKey: DEFAULT_API_KEY,
})

/**
 * 기본 주소(localhost:8080)면 Vite 프록시를 타도록 상대 경로를 쓴다.
 * 다른 주소로 바꿨을 때만 절대 경로를 쓰며, 이 경우 서버 CORS 설정이 필요하다.
 */
export function apiUrl(path: string, baseUrl = connectionStore.get().baseUrl): string {
  const base = baseUrl.trim().replace(/\/+$/, '')
  if (base === '' || base === DEFAULT_BASE_URL) return path
  return base + path
}

/** 사이드바 하단에 보여 줄 짧은 주소 (프로토콜 제거) */
export function displayHost(baseUrl: string): string {
  return baseUrl.replace(/^https?:\/\//, '').replace(/\/+$/, '')
}
