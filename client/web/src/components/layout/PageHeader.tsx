import type { ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import s from './Layout.module.css'

/** 본문 상단 44px 헤더: 브레드크럼 HubDash / 화면명 + 탭 + 오른쪽 주요 버튼 */
export function PageHeader({
  title,
  tabs,
  children,
  hideOnMobile,
  sub,
  titleTo,
}: {
  /** 없으면 'HubDash'만 (홈, 이슈 #177) */
  title?: string
  /** 가운데 칸을 눌렀을 때 갈 곳 (기본: 지금 화면의 첫 화면) */
  titleTo?: string
  /** 브레드크럼 세 번째 칸 (예: 메모 제목) */
  sub?: string
  tabs?: ReactNode
  children?: ReactNode
  /** 모바일 상단 헤더와 내용이 겹치면 숨긴다 */
  hideOnMobile?: boolean
}) {
  const { pathname } = useLocation()
  const base = '/' + (pathname.split('/')[1] ?? '')
  return (
    <header className={[s.header, ((!tabs && !children) || hideOnMobile) && s.bare].filter(Boolean).join(' ')}>
      <div className={s.crumb}>
        {/* 'HubDash'는 홈, 가운데 칸은 그 화면 첫 화면으로 (이슈 #132) */}
        <Link to="/" className={s.crumbLink}>
          HubDash
        </Link>
        {!title ? null : sub ? (
          <>
            <span>/</span>
            <Link to={titleTo ?? base} className={`${s.crumbMid} ${s.crumbLink}`}>
              {title}
            </Link>
            <span className={s.crumbSep}>/</span>
            <span className="ellipsis" style={{ maxWidth: 360 }}>
              {sub}
            </span>
          </>
        ) : (
          <>
            <span>/</span>
            <Link to={titleTo ?? base} className={s.crumbLink}>
              {title}
            </Link>
          </>
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
