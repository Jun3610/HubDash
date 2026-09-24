import { Plus, Search } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useProfile } from '../../api/user'
import { useToday } from '../../hooks/useToday'
import { formatHeaderDate } from '../../lib/date'
import { cx } from '../ui'
import { GlobalBanners } from './Banners'
import s from './Layout.module.css'
import { NAV, orderedNav, SETTINGS_NAV } from './nav'
import { navOrderStore } from '../../config/prefs'
import { useStore } from '../../lib/storage'
import { QuickRecordProvider } from './QuickRecord'
import { useQuickRecord } from './quickContext'
import { SearchPalette } from './SearchPalette'
import { useSearchHotkey } from './useSearchHotkey'
import { Sidebar } from './Sidebar'
import { initials } from '../../lib/format'
import { CoursePeek } from '../../pages/CoursePage'
import { SettingsPeek } from '../../pages/SettingsPage'
import { usePeekTo, withParam, type PeekKey } from './peek'

const MOBILE_TABS = [
  NAV.find((n) => n.key === 'home')!,
  NAV.find((n) => n.key === 'health')!,
  { ...NAV.find((n) => n.key === 'pknu')!, label: 'PKNU' },
  NAV.find((n) => n.key === 'study')!,
]

export function AppLayout() {
  const [searchOpen, setSearchOpen] = useState(false)
  const openSearch = useCallback(() => setSearchOpen(true), [])
  const closeSearch = useCallback(() => setSearchOpen(false), [])
  useSearchHotkey(openSearch)
  // ⌘/Ctrl + 1~6 → 사이드바에 보이는 순서대로 이동 (이슈 #211, 설정의 메뉴 순서를 따른다)
  const navigate = useNavigate()
  const navOrder = useStore(navOrderStore)
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (!(e.metaKey || e.ctrlKey) || e.shiftKey || e.altKey) return
      const n = Number(e.key)
      const items = orderedNav(navOrder)
      if (!Number.isInteger(n) || n < 1 || n > items.length) return
      e.preventDefault()
      navigate(items[n - 1].to)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [navigate, navOrder])

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
        <PeekHost />
      </div>
    </QuickRecordProvider>
  )
}

/** 주소의 ?course= / ?settings= 를 보고 어느 화면 위에서든 작은 창을 띄운다 (이슈 #148) */
function PeekHost() {
  const { pathname, search } = useLocation()
  const navigate = useNavigate()
  const params = new URLSearchParams(search)
  const courseId = Number(params.get('course'))
  const settings = params.get('settings')
  const close = (key: PeekKey) => navigate({ pathname, search: withParam(search, key, null) }, { replace: true })
  return (
    <>
      {courseId > 0 && <CoursePeek key={courseId} courseId={courseId} onClose={() => close('course')} />}
      {settings && <SettingsPeek section={settings} onClose={() => close('settings')} />}
    </>
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
  const { pathname, search } = useLocation()
  const peekTo = usePeekTo()
  const Settings = SETTINGS_NAV.icon
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
      <Link
        to={peekTo('settings', 1)}
        className={cx(s.tab, new URLSearchParams(search).has('settings') && s.tabActive)}
      >
        <Settings size={20} strokeWidth={1.7} />
        More
      </Link>
    </nav>
  )
}
