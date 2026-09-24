import { useQueryClient } from '@tanstack/react-query'
import { BookMarked, ChevronDown, ChevronLeft, ChevronRight, Plus, Search, Settings } from 'lucide-react'
import { useRef, useState } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import { connectionStatus } from '../../api/client'
import { useProfile } from '../../api/user'
import { connectionStore, displayHost } from '../../config/connection'
import { pinnedLinksStore, sidebarSemesterStore } from '../../config/prefs'
import { ACTIVITY_LABEL, useActivity } from '../../hooks/useActivity'
import { useAllHubLinks } from '../../hooks/useHub'
import { useSemesterBundle, useSemesters } from '../../hooks/usePknu'
import { useToday } from '../../hooks/useToday'
import { formatHeaderDate, formatShortDate, shiftDate, weekDays } from '../../lib/date'
import { initials } from '../../lib/format'
import { browserUrl } from '../../lib/url'
import { currentStreak, heatLevel } from '../../lib/heatmap'
import { activityOn } from '../../lib/select/activity'
import { useStore } from '../../lib/storage'
import { SemesterModal } from '../../pages/PknuPage'
import { cx } from '../ui'
import { usePeekTo } from './peek'
import { orderedNav } from './nav'
import { navOrderStore } from '../../config/prefs'
import s from './Sidebar.module.css'

export function Sidebar({ onSearch }: { onSearch: () => void }) {
  const today = useToday()
  const { pathname, search } = useLocation()
  const peekTo = usePeekTo()
  const profile = useProfile()
  const conn = useStore(connectionStore)
  const status = useStore(connectionStatus)
  const pinned = useStore(pinnedLinksStore)
  const qc = useQueryClient()

  const semPick = useStore(sidebarSemesterStore)
  const navOrder = useStore(navOrderStore)
  const semList = useSemesters().data?.content
  // 저장해 둔 학기가 지워졌으면 현재 학기로
  const pknu = useSemesterBundle(semPick != null && semList?.some((x) => x.id === semPick) ? semPick : null)
  const hub = useAllHubLinks()
  const activity = useActivity()

  const [semOpen, setSemOpen] = useState(true)
  const [pinOpen, setPinOpen] = useState(true)
  const [menuOpen, setMenuOpen] = useState(false)
  const [addingSem, setAddingSem] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)
  // 이번 주 기록: ◀ ▶로 주를 넘기고, 날짜를 누르면 그날 기록을 펼친다 (이슈 #132)
  const [weekOffset, setWeekOffset] = useState(0)
  const [picked, setPicked] = useState<string | null>(null)

  const counts: Partial<Record<string, number>> = {}

  const days = weekDays(shiftDate(today, weekOffset * 7))
  const weekCounts = days.map((d) => activity.counts.get(d) ?? 0)
  // 한 주 안에서만 비교하면 늘 최고 단계가 되므로 최근 4주 최댓값을 기준으로 한다
  const weekMax = Math.max(...Array.from({ length: 28 }, (_, i) => activity.counts.get(shiftDate(today, -i)) ?? 0))
  const weekTotal = weekCounts.reduce((a, b) => a + b, 0)
  const streak = currentStreak(activity.counts, today)
  const weekLabel =
    weekOffset === 0
      ? '이번 주 기록'
      : weekOffset === -1
        ? '지난 주 기록'
        : `${formatShortDate(days[0])} ~ ${formatShortDate(days[6])}`
  const pickedItems = picked ? activityOn(activity.sources, picked) : []
  const moveWeek = (d: number) => {
    setWeekOffset((w) => Math.min(0, w + d))
    setPicked(null)
  }

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
        <Link to="/" className={s.home} aria-label="홈으로">
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
        </Link>
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
              <button type="button" className={s.weekNav} aria-label="이전 주" onClick={() => moveWeek(-1)}>
                <ChevronLeft size={12} strokeWidth={2.2} />
              </button>
              <span>{weekLabel}</span>
              <button
                type="button"
                className={s.weekNav}
                aria-label="다음 주"
                disabled={weekOffset === 0}
                onClick={() => moveWeek(1)}
              >
                <ChevronRight size={12} strokeWidth={2.2} />
              </button>
              <span className={s.weekTotal}>{weekTotal}건</span>
            </div>
            <div className={s.weekGrid} aria-label={`${weekLabel} 요일별 기록 수`}>
              {days.map((d, i) => (
                <button
                  type="button"
                  key={d}
                  className={s.weekCell}
                  data-level={d > today ? 0 : heatLevel(weekCounts[i], weekMax)}
                  data-today={d === today}
                  data-future={d > today}
                  data-picked={d === picked}
                  disabled={d > today}
                  aria-pressed={d === picked}
                  aria-label={`${d} ${weekCounts[i]}건`}
                  title={`${d} · ${weekCounts[i]}건`}
                  onClick={() => setPicked((p) => (p === d ? null : d))}
                />
              ))}
            </div>
            <div className={s.weekDays} aria-hidden="true">
              {['월', '화', '수', '목', '금', '토', '일'].map((d) => (
                <span key={d}>{d}</span>
              ))}
            </div>
            {picked && (
              <div className={s.dayList} aria-live="polite">
                <span className={s.dayTitle}>{formatHeaderDate(picked)}</span>
                {pickedItems.length === 0 ? (
                  <span className={s.subEmpty} style={{ padding: 0 }}>
                    기록이 없어요
                  </span>
                ) : (
                  pickedItems.map((it, i) => (
                    <Link key={i} to={it.to} className={s.dayItem}>
                      <span className={s.dayDomain}>{ACTIVITY_LABEL[it.domain]}</span>
                      <span className="ellipsis">{it.text}</span>
                    </Link>
                  ))
                )}
              </div>
            )}
            <div className={s.streak}>
              <b>{streak}일</b> 연속 기록 중
            </div>
          </div>
        </div>

        <ul className={s.menu}>
          {orderedNav(navOrder).map((n) => {
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
            학기
            {pknu.courses.length > 0 && <span className={s.sectionMeta}>{credits}학점</span>}
          </button>
          <div className={s.semRow}>
            {pknu.semesters.length > 0 && (
              // 고른 학기가 없으면 지금 학기를 보여 준다 (이슈 #148에서 '현재 학기' 항목은 뺐다)
              <select
                className={s.semSelect}
                aria-label="사이드바에 보여 줄 학기"
                value={String(pknu.semester?.id ?? '')}
                onChange={(e) => sidebarSemesterStore.set(Number(e.target.value))}
              >
                {pknu.semesters.map((sem) => (
                  <option key={sem.id} value={sem.id}>
                    {sem.name}
                  </option>
                ))}
              </select>
            )}
            <button
              type="button"
              className={s.semAdd}
              aria-label="학기 추가"
              title="학기 추가"
              onClick={() => setAddingSem(true)}
            >
              <Plus size={13} strokeWidth={2} />
            </button>
          </div>
          {semOpen &&
            (pknu.courses.length === 0 ? (
              <span className={s.subEmpty}>{pknu.isLoading ? '불러오는 중…' : '등록된 과목이 없어요'}</span>
            ) : (
              pknu.courses.map((course) => (
                <Link key={course.id} to={peekTo('course', course.id)} className={s.subItem}>
                  <span className={s.dot} />
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
                <a key={l.id} href={browserUrl(l.url)} target="_blank" rel="noopener noreferrer" className={s.subItem}>
                  <BookMarked size={14} strokeWidth={1.8} color="var(--sb-muted)" aria-hidden="true" />
                  <span className={s.subLabel}>{l.title}</span>
                </a>
              ))
            ))}
        </div>
      </div>

      {addingSem && (
        <SemesterModal onClose={() => setAddingSem(false)} onSaved={(x) => sidebarSemesterStore.set(x.id)} />
      )}

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
          <Link
            to={peekTo('settings', 1)}
            aria-label="설정"
            className={cx(s.settings, new URLSearchParams(search).has('settings') && s.settingsActive)}
          >
            <Settings size={15} strokeWidth={1.8} />
          </Link>
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
  const peekTo = usePeekTo()
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
        <Link role="menuitem" to={peekTo('settings', 1)} onClick={onClose} className={s.subItem}>
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
