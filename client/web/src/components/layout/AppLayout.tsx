import { Plus, Search } from 'lucide-react'
import { useCallback, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useProfile } from '../../api/user'
import { useToday } from '../../hooks/useToday'
import { formatHeaderDate } from '../../lib/date'
import { cx } from '../ui'
import { GlobalBanners } from './Banners'
import s from './Layout.module.css'
import { NAV, SETTINGS_NAV } from './nav'
import { QuickRecordProvider } from './QuickRecord'
import { useQuickRecord } from './quickContext'
import { SearchPalette } from './SearchPalette'
import { useSearchHotkey } from './useSearchHotkey'
import { Sidebar } from './Sidebar'
import { initials } from '../../lib/format'

const MOBILE_TABS = [
  NAV.find((n) => n.key === 'home')!,
  NAV.find((n) => n.key === 'health')!,
  { ...NAV.find((n) => n.key === 'pknu')!, label: '학업' },
  NAV.find((n) => n.key === 'hub')!,
  { ...SETTINGS_NAV, label: '더보기' },
]

export function AppLayout() {
  const [searchOpen, setSearchOpen] = useState(false)
  const openSearch = useCallback(() => setSearchOpen(true), [])
  const closeSearch = useCallback(() => setSearchOpen(false), [])
  useSearchHotkey(openSearch)

  return (
    <QuickRecordProvider>
      <div className={s.app}>
        <div className={s.desktopOnly}>
          <Sidebar onSearch={openSearch} />
        </div>
        <main className={s.main}>
          <MobileHeader onSearch={openSearch} />
          <div className={s.banners}>
            <GlobalBanners />
          </div>
          <Outlet />
        </main>
        <MobileTabBar />
        <SearchPalette open={searchOpen} onClose={closeSearch} />
      </div>
    </QuickRecordProvider>
  )
}

function MobileHeader({ onSearch }: { onSearch: () => void }) {
  const today = useToday()
  const profile = useProfile()
  const quick = useQuickRecord()
  const { pathname } = useLocation()
  const current = [...NAV, SETTINGS_NAV].find((n) => (n.to === '/' ? pathname === '/' : pathname.startsWith(n.to)))
  return (
    <header className={s.mobileHeader}>
      <div className={s.mAvatar} aria-hidden="true">
        {initials(profile.data?.displayName)}
      </div>
      <div className={s.mTitle}>
        <span>{!current || current.key === 'home' ? '오늘' : current.crumb}</span>
        <span>{formatHeaderDate(today)}</span>
      </div>
      <button type="button" className={s.mBtn} aria-label="검색" onClick={onSearch}>
        <Search size={18} strokeWidth={1.8} />
      </button>
      <button type="button" className={cx(s.mBtn, s.mPrimary)} aria-label="빠른 기록" onClick={quick}>
        <Plus size={20} strokeWidth={2} />
      </button>
    </header>
  )
}

function MobileTabBar() {
  const { pathname } = useLocation()
  return (
    <nav aria-label="하단 탭" className={s.tabbar}>
      {MOBILE_TABS.map((t) => {
        const active = t.to === '/' ? pathname === '/' : pathname.startsWith(t.to)
        const Icon = t.icon
        return (
          <NavLink key={t.key} to={t.to} end={t.to === '/'} className={cx(s.tab, active && s.tabActive)}>
            <Icon size={20} strokeWidth={1.7} />
            {t.label}
          </NavLink>
        )
      })}
    </nav>
  )
}
