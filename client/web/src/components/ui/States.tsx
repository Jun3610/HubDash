import { AlertTriangle, RotateCw } from 'lucide-react'
import type { ReactNode } from 'react'
import { ApiError } from '../../api/client'
import { Button } from './Button'
import s from './Feedback.module.css'
import { cx } from './cx'

export function Skeleton({ lines = 3 }: { lines?: number }) {
  const widths = ['60%', '90%', '75%', '85%', '50%', '70%']
  return (
    <div className={s.skel} aria-busy="true" aria-label="불러오는 중">
      {Array.from({ length: lines }, (_, i) => (
        <div key={i} className={s.skelLine} style={{ width: widths[i % widths.length] }} />
      ))}
    </div>
  )
}

export function EmptyState({
  title = '아직 기록이 없어요',
  description,
  action,
  compact,
}: {
  title?: string
  description?: ReactNode
  action?: ReactNode
  compact?: boolean
}) {
  if (compact) {
    return (
      <div className={s.compactEmpty}>
        {title}
        {action && <> {action}</>}
      </div>
    )
  }
  return (
    <div className={s.empty}>
      <span className={s.emptyTitle}>{title}</span>
      {description && <span className={s.emptyDesc}>{description}</span>}
      {action && <div style={{ marginTop: 4 }}>{action}</div>}
    </div>
  )
}

/** 카드 안 조회 오류. 401·네트워크는 전역 배너가 이미 설명하므로 짧게만 */
export function InlineError({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const e = error instanceof ApiError ? error : null
  const global = e?.kind === 'network' || e?.kind === 'unauthorized'
  return (
    <div className={cx(s.inlineError)} role="alert">
      <AlertTriangle size={14} strokeWidth={1.8} color="var(--yellow)" />
      {e?.errorCode && !global && <code>{e.errorCode}</code>}
      <span className="ellipsis">{global ? '불러오지 못했어요' : (e?.message ?? '불러오지 못했어요')}</span>
      {onRetry && (
        <Button variant="link" size="sm" onClick={onRetry} icon={<RotateCw size={12} />}>
          다시 시도
        </Button>
      )}
    </div>
  )
}

/**
 * 조회 상태 분기: 불러오는 중 → 스켈레톤 / 오류 → InlineError / 빈 목록 → empty / 그 외 children
 */
export function QueryState({
  loading,
  error,
  empty,
  onRetry,
  lines,
  emptyView,
  children,
}: {
  loading: boolean
  error: unknown
  empty?: boolean
  onRetry?: () => void
  lines?: number
  emptyView?: ReactNode
  children: ReactNode
}) {
  if (loading) return <Skeleton lines={lines} />
  if (error) return <InlineError error={error} onRetry={onRetry} />
  if (empty) return <>{emptyView ?? <EmptyState compact />}</>
  return <>{children}</>
}
