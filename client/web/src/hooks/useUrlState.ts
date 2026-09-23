import { useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'

/** 쿼리스트링에 저장되는 화면 상태 (탭, 날짜, 선택한 항목 등) — 새로고침·뒤로가기에도 유지 */
export function useUrlState(key: string, fallback: string): [string, (v: string | null) => void] {
  const [params, setParams] = useSearchParams()
  const value = params.get(key) ?? fallback
  const set = useCallback(
    (v: string | null) =>
      setParams(
        (prev) => {
          const next = new URLSearchParams(prev)
          if (v === null || v === fallback) next.delete(key)
          else next.set(key, v)
          return next
        },
        { replace: true },
      ),
    [key, fallback, setParams],
  )
  return [value, set]
}
