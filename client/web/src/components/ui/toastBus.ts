import { createContext, useContext } from 'react'

export interface ToastApi {
  success(title: string, message?: string): void
  error(title: string, code?: string, message?: string): void
  /** ApiError의 errorCode와 message를 그대로 보여 준다. 401·네트워크는 배너가 맡으므로 건너뛴다 */
  apiError(err: unknown, title?: string): void
}

export const ToastContext = createContext<ToastApi | null>(null)

// React 밖(QueryClient MutationCache)에서도 토스트를 띄우기 위한 연결점
let globalApi: ToastApi | null = null

export function setGlobalToast(api: ToastApi) {
  globalApi = api
}

/** 뮤테이션 실패를 토스트로. success:false면 errorCode와 message를 그대로 보여 준다 */
export function toastApiError(err: unknown, title?: string) {
  globalApi?.apiError(err, title)
}

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('ToastProvider 밖에서 useToast를 썼습니다')
  return ctx
}
