import { useCallback, useState } from 'react'

/**
 * 체크박스처럼 즉시 바뀌어 보여야 하는 값. 서버 응답 + 다시 조회가 끝나면(onSettled) 덮어쓴 값을 지워
 * 서버 값으로 돌아가고, 실패하면 자연히 원래 값으로 되돌아간다.
 */
export function useOptimistic<K>() {
  const [over, setOver] = useState<Map<K, boolean>>(() => new Map())
  const value = useCallback((key: K, server: boolean) => (over.has(key) ? over.get(key)! : server), [over])
  const set = useCallback((key: K, v: boolean) => setOver((m) => new Map(m).set(key, v)), [])
  const clear = useCallback(
    (key: K) =>
      setOver((m) => {
        const next = new Map(m)
        next.delete(key)
        return next
      }),
    [],
  )
  return { value, set, clear }
}
