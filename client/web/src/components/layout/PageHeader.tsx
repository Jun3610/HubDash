import type { ReactNode } from 'react'
import s from './Layout.module.css'

/** 본문 상단 44px 헤더: 브레드크럼 HubDash / 화면명 + 탭 + 오른쪽 주요 버튼 */
export function PageHeader({
  title,
  tabs,
  children,
  hideOnMobile,
  sub,
}: {
  title: string
  /** 브레드크럼 세 번째 칸 (예: 메모 제목) */
  sub?: string
  tabs?: ReactNode
  children?: ReactNode
  /** 모바일 상단 헤더와 내용이 겹치면 숨긴다 */
  hideOnMobile?: boolean
}) {
  return (
    <header className={[s.header, ((!tabs && !children) || hideOnMobile) && s.bare].filter(Boolean).join(' ')}>
      <div className={s.crumb}>
        <span>HubDash</span>
        <span>/</span>
        {sub ? (
          <>
            <span className={s.crumbMid}>{title}</span>
            <span className={s.crumbSep}>/</span>
            <span className="ellipsis" style={{ maxWidth: 360 }}>
              {sub}
            </span>
          </>
        ) : (
          <span>{title}</span>
        )}
      </div>
      {tabs && <div className={s.headerTabs}>{tabs}</div>}
      {children && <div className={s.headerRight}>{children}</div>}
    </header>
  )
}

export function PageContent({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={[s.content, className].filter(Boolean).join(' ')}>{children}</div>
}
