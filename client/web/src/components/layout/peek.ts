import { useLocation, type To } from 'react-router-dom'

/**
 * 작은 창은 지금 화면 주소에 쿼리를 붙여 연다 (이슈 #148) — 어느 화면 위에서든 뜨고, 뒤로 가기로 닫힌다.
 * ?course=<id> 과목, ?settings=<구역 id 또는 1> 설정
 */
export type PeekKey = 'course' | 'settings'

export function withParam(search: string, key: PeekKey, value: string | number | null): string {
  const p = new URLSearchParams(search)
  if (value === null) p.delete(key)
  else p.set(key, String(value))
  const s = p.toString()
  return s ? `?${s}` : ''
}

/** 지금 화면을 그대로 두고 작은 창만 여는 링크 대상 */
export function usePeekTo(): (key: PeekKey, value: string | number) => To {
  const { pathname, search } = useLocation()
  return (key, value) => ({ pathname, search: withParam(search, key, value) })
}
