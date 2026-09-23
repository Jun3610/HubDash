import type { ReactNode } from 'react'
import s from './Layout.module.css'

/** 본문 상단 44px 헤더: 브레드크럼 HubDash / 화면명 + 탭 + 오른쪽 주요 버튼 */
export function PageHeader({ title, tabs, children }: { title: string; tabs?: ReactNode; children?: ReactNode }) {
  return (
    <header className={[s.header, !tabs && !children && s.bare].filter(Boolean).join(' ')}>
      <div className={s.crumb}>
        <span>HubDash</span>
        <span>/</span>
        <span>{title}</span>
      </div>
      {tabs && <div className={s.headerTabs}>{tabs}</div>}
      {children && <div className={s.headerRight}>{children}</div>}
    </header>
  )
}

export function PageContent({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={[s.content, className].filter(Boolean).join(' ')}>{children}</div>
}
