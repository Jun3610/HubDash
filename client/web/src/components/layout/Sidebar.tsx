import { useQueryClient } from '@tanstack/react-query'
import { BookMarked, ChevronDown, Search, Settings } from 'lucide-react'
import { useRef, useState } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import { connectionStatus } from '../../api/client'
import { useProfile } from '../../api/user'
import { connectionStore, displayHost } from '../../config/connection'
import { pinnedLinksStore } from '../../config/prefs'
import { useActivity } from '../../hooks/useActivity'
import { useAllHubLinks } from '../../hooks/useHub'
import { courseColor, useSemesterBundle } from '../../hooks/usePknu'
import { useToday } from '../../hooks/useToday'
import { shiftDate, weekDays } from '../../lib/date'
import { initials } from '../../lib/format'
import { currentStreak, heatLevel } from '../../lib/heatmap'
import { useStore } from '../../lib/storage'
import { cx } from '../ui'
import { NAV } from './nav'
import s from './Sidebar.module.css'

export function Sidebar({ onSearch }: { onSearch: () => void }) {
  const today = useToday()
  const { pathname } = useLocation()
  const profile = useProfile()
  const conn = useStore(connectionStore)
  const status = useStore(connectionStatus)
  const pinned = useStore(pinnedLinksStore)
  const qc = useQueryClient()

  const pknu = useSemesterBundle()
  const hub = useAllHubLinks()
  const activity = useActivity()

  const [semOpen, setSemOpen] = useState(true)
  const [pinOpen, setPinOpen] = useState(true)
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  const counts: Partial<Record<string, number>> = {
    hub: hub.links.length || undefined,
  }

  const days = weekDays(today)
  const weekCounts = days.map((d) => activity.counts.get(d) ?? 0)
  // 한 주 안에서만 비교하면 늘 최고 단계가 되므로 최근 4주 최댓값을 기준으로 한다
  const weekMax = Math.max(...Array.from({ length: 28 }, (_, i) => activity.counts.get(shiftDate(today, -i)) ?? 0))
  const weekTotal = weekCounts.reduce((a, b) => a + b, 0)
  const streak = currentStreak(activity.counts, today)

  const courseRows = pknu.courses.map((c, i) => ({ course: c, color: courseColor(i) }))
  const credits = pknu.courses.reduce((sum, c) => sum + c.credit, 0)
  const pinnedLinks = hub.links.filter((l) => pinned.includes(l.id))

  const connColor =
    status === 'online' ? 'var(--sb-online)' : status === 'unknown' ? 'var(--sb-offline)' : 'var(--sb-error)'
  const connText =
    status === 'online'
      ? 'API 연결됨'
      : status === 'unauthorized'
        ? 'API 키 오류'
        : status === 'offline'
          ? '연결 끊김'
          : '확인 중'

  return (
    <nav aria-label="주 메뉴" className={s.nav}>
      <div className={s.band}>
        <div className={s.logo} aria-hidden="true">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.4"
            strokeLinecap="round"
          >
            <path d="M6 4v16M18 4v16M6 12h12" />
          </svg>
        </div>
        <div className={s.repo}>
          <span>Jun3610</span>
          <span>/</span>
          <b>HubDash</b>
        </div>
        <div ref={menuRef} style={{ position: 'relative' }}>
          <button
            type="button"
            className={s.sbBtn}
            aria-label="작업 공간 메뉴"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((v) => !v)}
          >
            <ChevronDown size={12} strokeWidth={2.2} />
          </button>
          {menuOpen && (
            <WorkspaceMenu
              onClose={() => setMenuOpen(false)}
              onRefresh={() => {
                void qc.invalidateQueries()
                setMenuOpen(false)
              }}
            />
          )}
        </div>
      </div>

      <div className={s.scroll}>
        <div className={s.top}>
          <button type="button" className={s.search} onClick={onSearch}>
            <Search size={14} strokeWidth={1.8} />
            <span style={{ flexGrow: 1 }}>
              <span className={s.kbd}>/</span> 로 검색
            </span>
            <span className="mono" style={{ fontSize: 11 }}>
              ⌘K
            </span>
          </button>

          <div className={s.week}>
            <div className={s.weekHead}>
              <span>이번 주 기록</span>
              <span>{weekTotal}건</span>
            </div>
            <div className={s.weekGrid} aria-label="이번 주 요일별 기록 수" role="img">
              {days.map((d, i) => (
                <span
                  key={d}
                  className={s.weekCell}
                  data-level={d > today ? 0 : heatLevel(weekCounts[i], weekMax)}
                  data-today={d === today}
                  data-future={d > today}
                  title={`${d} · ${weekCounts[i]}건`}
                />
              ))}
            </div>
            <div className={s.weekDays} aria-hidden="true">
              {['월', '화', '수', '목', '금', '토', '일'].map((d) => (
                <span key={d}>{d}</span>
              ))}
            </div>
            <div className={s.streak}>
              <b>{streak}일</b> 연속 기록 중
            </div>
          </div>
        </div>

        <ul className={s.menu}>
          {NAV.map((n) => {
            const active = n.to === '/' ? pathname === '/' : pathname.startsWith(n.to)
            const count = counts[n.key]
            const Icon = n.icon
            return (
              <li key={n.key}>
                {active && <span className={s.bar} aria-hidden="true" />}
                <NavLink to={n.to} end={n.to === '/'} className={cx(s.item, active && s.itemActive)}>
                  <Icon size={16} strokeWidth={1.7} aria-hidden="true" />
                  <span className={s.itemLabel}>{n.label}</span>
                  {count !== undefined && <span className={s.count}>{count}</span>}
                </NavLink>
              </li>
            )
          })}
        </ul>

        <div className={s.divider} />

        <div className={s.section}>
          <button
            type="button"
            className={cx(s.sectionHead, !semOpen && s.collapsed)}
            aria-expanded={semOpen}
            onClick={() => setSemOpen((v) => !v)}
          >
            <ChevronDown size={12} strokeWidth={2.2} aria-hidden="true" />
            {pknu.semester?.name ?? '현재 학기'}
            {pknu.courses.length > 0 && <span className={s.sectionMeta}>{credits}학점</span>}
          </button>
          {semOpen &&
            (courseRows.length === 0 ? (
              <span className={s.subEmpty}>{pknu.isLoading ? '불러오는 중…' : '등록된 과목이 없어요'}</span>
            ) : (
              courseRows.map(({ course, color }) => (
                <Link key={course.id} to={`/pknu/courses/${course.id}`} className={s.subItem}>
                  <span className={s.dot} style={{ background: color }} />
                  <span className={s.subLabel}>{course.name}</span>
                </Link>
              ))
            ))}
        </div>

        <div className={s.section} style={{ paddingTop: 12, paddingBottom: 12 }}>
          <button
            type="button"
            className={cx(s.sectionHead, !pinOpen && s.collapsed)}
            aria-expanded={pinOpen}
            onClick={() => setPinOpen((v) => !v)}
          >
            <ChevronDown size={12} strokeWidth={2.2} aria-hidden="true" />
            고정한 링크
          </button>
          {pinOpen &&
            (pinnedLinks.length === 0 ? (
              <span className={s.subEmpty}>허브에서 링크를 고정해 보세요</span>
            ) : (
              pinnedLinks.map((l) => (
                <a key={l.id} href={l.url} target="_blank" rel="noreferrer" className={s.subItem}>
                  <BookMarked size={14} strokeWidth={1.8} color="var(--sb-muted)" aria-hidden="true" />
                  <span className={s.subLabel}>{l.title}</span>
                </a>
              ))
            ))}
        </div>
      </div>

      <div className={s.footer}>
        <div className={s.me}>
          <div className={s.avatar} aria-hidden="true">
            {initials(profile.data?.displayName)}
            <span className={s.online} style={{ background: connColor }} />
          </div>
          <div className={s.meText}>
            <span className={cx(s.meName, 'ellipsis')}>{profile.data?.displayName ?? '—'}</span>
            <span className={s.meHandle}>@Jun3610</span>
          </div>
          <NavLink
            to="/settings"
            aria-label="설정"
            className={({ isActive }) => cx(s.settings, isActive && s.settingsActive)}
          >
            <Settings size={15} strokeWidth={1.8} />
          </NavLink>
        </div>
        <div className={s.conn}>
          <span className={s.connDot} style={{ background: connColor }} />
          <span className="mono ellipsis">{displayHost(conn.baseUrl)}</span>
          <span>{connText}</span>
        </div>
      </div>
    </nav>
  )
}

function WorkspaceMenu({ onClose, onRefresh }: { onClose: () => void; onRefresh: () => void }) {
  return (
    <>
      <div style={{ position: 'fixed', inset: 0, zIndex: 20 }} onClick={onClose} />
      <div
        role="menu"
        style={{
          position: 'absolute',
          right: 0,
          top: 30,
          zIndex: 21,
          width: 180,
          padding: 4,
          background: 'var(--sb-bg)',
          border: '1px solid var(--sb-border)',
          borderRadius: 6,
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Link role="menuitem" to="/settings" onClick={onClose} className={s.subItem}>
          설정 열기
        </Link>
        <button
          role="menuitem"
          type="button"
          onClick={onRefresh}
          className={s.subItem}
          style={{ background: 'transparent', border: 0, textAlign: 'left' }}
        >
          데이터 새로고침
        </button>
      </div>
    </>
  )
}
