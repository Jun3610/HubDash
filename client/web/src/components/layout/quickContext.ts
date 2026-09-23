import { createContext, useContext } from 'react'

export const QuickContext = createContext<() => void>(() => {})

/** 빠른 기록 모달 열기 */
export function useQuickRecord() {
  return useContext(QuickContext)
}
