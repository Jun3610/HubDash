import { X } from 'lucide-react'
import { useEffect, useRef, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { Button, IconButton } from './Button'
import s from './Feedback.module.css'
import { cx } from './cx'

export function Modal({
  open,
  onClose,
  title,
  children,
  footer,
  wide,
}: {
  open: boolean
  onClose: () => void
  title: ReactNode
  children: ReactNode
  footer?: ReactNode
  wide?: boolean
}) {
  const ref = useRef<HTMLDivElement>(null)
  // 부모가 다시 그려질 때마다 onClose가 새로 만들어져도 포커스를 처음으로 되돌리지 않게 ref로 둔다 (이슈 #193)
  const closeRef = useRef(onClose)
  useEffect(() => {
    closeRef.current = onClose
  })

  useEffect(() => {
    if (!open) return
    const prev = document.activeElement as HTMLElement | null
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') closeRef.current()
    }
    document.addEventListener('keydown', onKey)
    // 첫 입력 칸에 포커스
    const first = ref.current?.querySelector<HTMLElement>('input, select, textarea, button:not([aria-label="닫기"])')
    first?.focus()
    return () => {
      document.removeEventListener('keydown', onKey)
      prev?.focus?.()
    }
  }, [open])

  if (!open) return null
  return createPortal(
    <div className={s.overlay} onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div
        ref={ref}
        role="dialog"
        aria-modal="true"
        aria-label={typeof title === 'string' ? title : undefined}
        className={cx(s.modal, wide && s.modalWide)}
      >
        <div className={s.modalHeader}>
          <span className={s.modalTitle}>{title}</span>
          <IconButton label="닫기" size="sm" style={{ marginLeft: 'auto' }} onClick={onClose}>
            <X size={14} />
          </IconButton>
        </div>
        <div className={s.modalBody}>{children}</div>
        {footer && <div className={s.modalFooter}>{footer}</div>}
      </div>
    </div>,
    document.body,
  )
}

export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = '삭제',
  onConfirm,
  onClose,
  busy,
}: {
  open: boolean
  title: string
  message?: ReactNode
  confirmLabel?: string
  onConfirm: () => void
  onClose: () => void
  busy?: boolean
}) {
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      footer={
        <>
          <Button onClick={onClose}>취소</Button>
          <Button variant="danger" onClick={onConfirm} disabled={busy}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      {message && <p style={{ fontSize: 13 }}>{message}</p>}
    </Modal>
  )
}
