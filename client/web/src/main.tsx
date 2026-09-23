import { MutationCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import { ApiError } from './api/client'
import { toastApiError, ToastProvider } from './components/ui'
import './styles/tokens.css'
import './styles/global.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: true,
      // 서버가 꺼져 있거나 키가 틀린 경우, 요청 자체가 틀린 경우(4xx)는 재시도해도 소용없다
      retry: (count, err) => {
        if (err instanceof ApiError && (err.kind !== 'server' || (err.status ?? 500) < 500)) return false
        return count < 1
      },
    },
  },
  // 모든 저장·삭제 실패를 한곳에서 토스트로 (401·네트워크는 배너가 맡는다)
  mutationCache: new MutationCache({
    onError: (err, _vars, _ctx, mutation) => {
      if (mutation.meta?.silent) return
      toastApiError(err)
    },
  }),
})

async function start() {
  if (import.meta.env.VITE_USE_MOCK === 'true') {
    const { worker } = await import('./mocks/browser')
    await worker.start({ onUnhandledRequest: 'bypass', quiet: true })
  }
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <App />
        </ToastProvider>
      </QueryClientProvider>
    </StrictMode>,
  )
}

void start()
