import { AlertTriangle, CheckCircle2, X } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ApiError } from '../../api/client'
import s from './Feedback.module.css'
import { cx } from './cx'
import { IconButton } from './Button'
import { setGlobalToast, ToastContext, type ToastApi } from './toastBus'

interface ToastItem {
  id: number
  kind: 'success' | 'error'
  title: string
  code?: string
  message?: string
}

let seq = 0

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])

  const dismiss = useCallback((id: number) => setItems((xs) => xs.filter((x) => x.id !== id)), [])

  const push = useCallback(
    (t: Omit<ToastItem, 'id'>) => {
      const id = ++seq
      setItems((xs) => [...xs.slice(-3), { ...t, id }])
      setTimeout(() => dismiss(id), t.kind === 'error' ? 6000 : 2500)
    },
    [dismiss],
  )

  const api = useMemo<ToastApi>(
    () => ({
      success: (title, message) => push({ kind: 'success', title, message }),
      error: (title, code, message) => push({ kind: 'error', title, code, message }),
      apiError: (err, title = '저장하지 못했어요') => {
        if (err instanceof ApiError) {
          if (err.kind === 'unauthorized' || err.kind === 'network') return
          push({ kind: 'error', title, code: err.errorCode, message: err.message })
        } else {
          push({ kind: 'error', title, message: (err as Error)?.message })
        }
      },
    }),
    [push],
  )
  useEffect(() => setGlobalToast(api), [api])

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className={s.toasts} aria-live="polite">
        {items.map((t) => (
          <div key={t.id} className={cx(s.toast, t.kind === 'success' ? s.toastSuccess : s.toastError)} role="status">
            <span className={s.toastIcon}>
              {t.kind === 'success' ? (
                <CheckCircle2 size={16} strokeWidth={1.8} />
              ) : (
                <AlertTriangle size={16} strokeWidth={1.8} />
              )}
            </span>
            <div className={s.toastBody}>
              {t.code && <span className={s.toastCode}>{t.code}</span>}
              <span>{t.title}</span>
              {t.message && <span className={s.toastMsg}>{t.message}</span>}
            </div>
            <IconButton label="닫기" size="sm" onClick={() => dismiss(t.id)}>
              <X size={14} />
            </IconButton>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
