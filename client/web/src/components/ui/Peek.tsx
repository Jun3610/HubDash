import { X } from 'lucide-react'
import { useEffect, useRef, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { IconButton } from './Button'
import s from './Peek.module.css'

/**
 * 노션 미리보기처럼 뒤 화면을 어둡게·흐리게 두고 가운데 작은 창으로 띄운다 (이슈 #148).
 * 페이지는 사이드바 메뉴 7개뿐이고 나머지 세부 화면은 전부 이 창으로 연다.
 */
export function Peek({
  label,
  onClose,
  actions,
  children,
}: {
  label: string
  onClose: () => void
  actions?: ReactNode
  children: ReactNode
}) {
  const ref = useRef<HTMLDivElement>(null)
  // 부모가 다시 그려질 때마다 onClose가 새로 만들어져도 창을 다시 초기화하지 않게 ref로 둔다
  const closeRef = useRef(onClose)
  useEffect(() => {
    closeRef.current = onClose
  })

  useEffect(() => {
    const prev = document.activeElement as HTMLElement | null
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== 'Escape') return
      // 맨 위 창만 닫는다 — 창 안에서 연 모달(끼니 입력 등)이나 겹쳐 연 창(설정)이 있으면 그것만
      const dialogs = document.querySelectorAll('[role="dialog"]')
      if (dialogs[dialogs.length - 1] !== ref.current) return
      closeRef.current()
    }
    document.addEventListener('keydown', onKey)
    ref.current?.focus()
    // 뒤 화면은 스크롤되지 않게
    const overflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = overflow
      prev?.focus?.()
    }
  }, [])

  return createPortal(
    <div className={s.overlay} onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div ref={ref} role="dialog" aria-modal="true" aria-label={label} tabIndex={-1} className={s.panel}>
        <div className={s.bar}>
          <div className={s.actions}>{actions}</div>
          <IconButton label="닫기" size="sm" onClick={onClose}>
            <X size={15} />
          </IconButton>
        </div>
        <div className={s.body}>{children}</div>
      </div>
    </div>,
    document.body,
  )
}
