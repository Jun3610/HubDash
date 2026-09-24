import { useIsMutating } from '@tanstack/react-query'
import { Eye, EyeOff, Plus, X } from 'lucide-react'
import { useState, type FormEvent, type ReactNode } from 'react'
import { WEEKLY_STATS, useRunWeeklyStat, type WeeklyStat } from '../api/analytics'
import { ApiError, connectionStatus, request } from '../api/client'
import type { BatchRunResult, UserProfile, UserSetting } from '../api/types'
import { useProfile, useSettings, useUpdateProfile, useUpdateSettings } from '../api/user'
import { PageHeader } from '../components/layout/PageHeader'
import { Button, Field, FormError, Input, QueryState, Select, Switch, Tag, Textarea, useToast } from '../components/ui'
import { connectionStore, DEFAULT_API_KEY, DEFAULT_BASE_URL } from '../config/connection'
import { DEFAULT_GOALS, goalsStore, type Goals } from '../config/goals'
import { accentStore, ACCENTS, AVATAR_COLORS, avatarColorStore, pledgesStore, type ThemeValue } from '../config/prefs'
import { useToday } from '../hooks/useToday'
import { shiftDate, weekStartOf, type LocalDate } from '../lib/date'
import { initials } from '../lib/format'
import { useStore } from '../lib/storage'
import s from './settings/Settings.module.css'

export default function SettingsPage() {
  const saving = useIsMutating()
  return (
    <>
      <PageHeader title="설정">
        <span className={s.status} style={{ color: 'var(--text-muted)' }}>
          <span className={s.dot} style={{ background: saving ? 'var(--yellow)' : 'var(--green)' }} />
          {saving ? '저장 중…' : '모든 변경 저장됨'}
        </span>
      </PageHeader>
      <div className={s.grid}>
        <div className={s.col}>
          <ProfileSection />
          <DisplaySection />
          <NotificationSection />
          <GoalsSection />
        </div>
        <div className={s.col}>
          <ApiSection />
          <BatchSection />
        </div>
      </div>
    </>
  )
}

function Section({
  title,
  path,
  right,
  id,
  children,
}: {
  title: string
  path?: string
  right?: ReactNode
  id?: string
  children: ReactNode
}) {
  return (
    <section className={s.section} id={id} aria-labelledby={id ? `${id}-title` : undefined}>
      <div className={s.sectionHead}>
        <h2 id={id ? `${id}-title` : undefined}>{title}</h2>
        {path && <span className={s.path}>{path}</span>}
        {right}
      </div>
      {children}
    </section>
  )
}

// ---- 프로필 ----

function ProfileSection() {
  const profile = useProfile()
  return (
    <Section title="프로필" path="/api/user/profile">
      <div className={s.body}>
        <QueryState loading={profile.isLoading} error={profile.error} onRetry={() => void profile.refetch()} lines={4}>
          {profile.data && <ProfileForm key={profile.data.updatedAt} profile={profile.data} />}
        </QueryState>
      </div>
    </Section>
  )
}

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function ProfileForm({ profile }: { profile: UserProfile }) {
  const [displayName, setDisplayName] = useState(profile.displayName ?? '')
  const [email, setEmail] = useState(profile.email ?? '')
  const [bio, setBio] = useState(profile.bio ?? '')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const avatar = useStore(avatarColorStore)
  const update = useUpdateProfile()
  const toast = useToast()

  const submit = (e: FormEvent) => {
    e.preventDefault()
    const next: Record<string, string> = {}
    if (!displayName.trim()) next.displayName = '표시 이름을 입력하세요'
    else if (displayName.length > 100) next.displayName = '100자 이하로 입력하세요'
    if (email && !EMAIL.test(email)) next.email = '이메일 형식이 아니에요'
    else if (email.length > 200) next.email = '200자 이하로 입력하세요'
    if (bio.length > 500) next.bio = `500자 이하로 입력하세요 (지금 ${bio.length}자)`
    setErrors(next)
    if (Object.keys(next).length) return
    update.mutate(
      { displayName: displayName.trim(), email: email.trim() || null, bio: bio.trim() || null },
      { onSuccess: () => toast.success('프로필을 저장했어요') },
    )
  }

  const nextColor = () => {
    const i = AVATAR_COLORS.indexOf(avatar as (typeof AVATAR_COLORS)[number])
    avatarColorStore.set(AVATAR_COLORS[(i + 1) % AVATAR_COLORS.length])
  }

  return (
    <form onSubmit={submit} noValidate style={{ display: 'contents' }}>
      <div className={s.avatarRow}>
        <div className={s.avatar} aria-hidden="true">
          {initials(displayName)}
        </div>
        <Button size="sm" onClick={nextColor}>
          이니셜 색 바꾸기
        </Button>
      </div>
      <div className={s.two}>
        <Field label="표시 이름" required error={errors.displayName}>
          <Input value={displayName} onChange={(e) => setDisplayName(e.target.value)} maxLength={100} />
        </Field>
        <Field label="이메일" error={errors.email}>
          <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} maxLength={200} />
        </Field>
      </div>
      <Field label="소개" error={errors.bio}>
        <Textarea rows={2} value={bio} onChange={(e) => setBio(e.target.value)} style={{ minHeight: 56 }} />
      </Field>
      <FormError error={update.error} />
      <div className={s.actions}>
        <Button type="submit" variant="primary" disabled={update.isPending}>
          {update.isPending ? '저장 중…' : '프로필 저장'}
        </Button>
      </div>
    </form>
  )
}

// ---- 화면 (테마, 언어, 강조색) ----

const THEMES: { key: ThemeValue; label: string }[] = [
  { key: 'DARK', label: '다크' },
  { key: 'LIGHT', label: '라이트' },
  { key: 'SYSTEM', label: '시스템' },
]

/** 서버 설정은 PUT에 전체 값이 필요해서 현재 값에 바뀐 것만 덮어 보낸다 */
function useSaveSetting() {
  const settings = useSettings()
  const update = useUpdateSettings()
  return {
    settings,
    update,
    save: (patch: Partial<UserSetting>) => {
      const cur = settings.data
      if (!cur) return
      update.mutate({
        theme: patch.theme ?? cur.theme,
        language: patch.language ?? cur.language,
        notificationEnabled: patch.notificationEnabled ?? cur.notificationEnabled,
      })
    },
  }
}

function DisplaySection() {
  const { settings, save } = useSaveSetting()
  const accent = useStore(accentStore)
  const theme = (settings.data?.theme ?? 'DARK').toUpperCase()
  return (
    <Section title="화면" path="/api/user/settings">
      <div className={s.body}>
        <QueryState loading={settings.isLoading} error={settings.error} onRetry={() => void settings.refetch()}>
          <div className={s.formGrid}>
            <span className={s.label} id="theme-label">
              테마
            </span>
            <div role="radiogroup" aria-labelledby="theme-label" className={s.seg}>
              {THEMES.map((t) => (
                <button
                  key={t.key}
                  type="button"
                  role="radio"
                  aria-checked={theme === t.key}
                  onClick={() => theme !== t.key && save({ theme: t.key })}
                >
                  {t.label}
                </button>
              ))}
            </div>

            <label className={s.label} htmlFor="lang">
              언어
            </label>
            <Select
              id="lang"
              style={{ width: 180 }}
              value={settings.data?.language ?? 'ko'}
              onChange={(e) => save({ language: e.target.value })}
            >
              <option value="ko">한국어</option>
              <option value="en">English</option>
            </Select>

            <span className={`${s.label} ${s.top}`} id="accent-label">
              강조색
            </span>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, minWidth: 0 }}>
              <div role="radiogroup" aria-labelledby="accent-label" className={s.accents}>
                {ACCENTS.map((a) => (
                  <button
                    key={a.key}
                    type="button"
                    role="radio"
                    aria-checked={accent === a.key}
                    aria-label={a.label}
                    className={s.accent}
                    onClick={() => accentStore.set(a.key)}
                  >
                    <span className={s.swatch} style={{ background: a.swatch }} />
                    {a.label}
                  </button>
                ))}
              </div>
              <div className={s.preview} aria-label="강조색 미리보기">
                <span className="muted" style={{ fontSize: 12 }}>
                  미리보기
                </span>
                <span className={s.previewHeat}>
                  {[0, 1, 2, 3, 4].map((l) => (
                    <span key={l} style={{ background: `var(--heat-${l})` }} />
                  ))}
                </span>
                <span className={s.previewBtn}>버튼</span>
                <span className={s.previewTab}>탭</span>
              </div>
              <span className={s.note}>
                버튼 · 탭 밑줄 · 기록 히트맵 · 사이드바 표시가 함께 바뀌어요. 이 브라우저에만 저장돼요.
              </span>
            </div>
          </div>
        </QueryState>
      </div>
    </Section>
  )
}

function NotificationSection() {
  const { settings, save } = useSaveSetting()
  return (
    <section className={s.section}>
      <div className={s.inline}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 2, flexGrow: 1 }}>
          <h2 id="noti-label">알림</h2>
          <span className={s.note} style={{ fontSize: 12 }}>
            리마인더 시각에 알림을 보내요 · notificationEnabled
          </span>
        </div>
        <Switch
          label="알림"
          checked={!!settings.data?.notificationEnabled}
          disabled={!settings.data}
          onChange={(v) => save({ notificationEnabled: v })}
        />
      </div>
    </section>
  )
}

// ---- 목표 · 학기 다짐 (서버에 없어서 이 브라우저에 저장) ----

const GOAL_FIELDS: { key: keyof Goals; label: string; unit: string }[] = [
  { key: 'calories', label: '칼로리', unit: 'kcal' },
  { key: 'proteinG', label: '단백질 이상', unit: 'g' },
  { key: 'carbsG', label: '탄수 이하', unit: 'g' },
  { key: 'fatG', label: '지방 이하', unit: 'g' },
  { key: 'sodiumMg', label: '나트륨', unit: 'mg' },
  { key: 'studyWeekMinutes', label: '주간 공부', unit: '분' },
]

function GoalsSection() {
  const goals = useStore(goalsStore)
  const pledges = useStore(pledgesStore)
  const [draft, setDraft] = useState('')

  const addPledge = (e: FormEvent) => {
    e.preventDefault()
    const v = draft.trim()
    if (!v || pledges.includes(v)) return
    pledgesStore.set([...pledges, v])
    setDraft('')
  }

  return (
    <Section title="목표 · 학기 다짐" path="이 브라우저에 저장">
      <div className={s.body}>
        <div className={s.goals}>
          {GOAL_FIELDS.map((g) => (
            <Field key={g.key} label={`${g.label} (${g.unit})`}>
              <Input
                type="number"
                mono
                min={0}
                value={goals[g.key]}
                onChange={(e) => {
                  const n = Number(e.target.value)
                  if (Number.isFinite(n) && n >= 0) goalsStore.set({ ...goals, [g.key]: n })
                }}
              />
            </Field>
          ))}
        </div>
        <div className={s.actions} style={{ justifyContent: 'space-between' }}>
          <span className={s.note}>홈과 건강 화면의 목표 대비 막대에 쓰여요.</span>
          <Button size="sm" variant="ghost" onClick={() => goalsStore.set(DEFAULT_GOALS)}>
            기본값으로
          </Button>
        </div>
        <div
          style={{
            borderTop: '1px solid var(--divider)',
            paddingTop: 12,
            display: 'flex',
            flexDirection: 'column',
            gap: 8,
          }}
        >
          <span className="muted" style={{ fontSize: 12 }}>
            학기 다짐 · 생활 화면 위에 태그로 보여요
          </span>
          <div className={s.pledges}>
            {pledges.length === 0 && <span className={s.note}>아직 다짐이 없어요</span>}
            {pledges.map((p) => (
              <Tag key={p} tone="yellow" size="lg">
                {p}
                <button
                  type="button"
                  className={s.pledgeRemove}
                  aria-label={`${p} 삭제`}
                  onClick={() => pledgesStore.set(pledges.filter((x) => x !== p))}
                >
                  <X size={12} />
                </button>
              </Tag>
            ))}
          </div>
          <form onSubmit={addPledge} className={s.keyRow}>
            <Input
              aria-label="새 다짐"
              placeholder="예: 주 3일 웨이트"
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              maxLength={40}
            />
            <Button type="submit" icon={<Plus size={14} />} disabled={!draft.trim()}>
              추가
            </Button>
          </form>
        </div>
      </div>
    </Section>
  )
}

// ---- API 연결 ----

type TestResult = { ok: true; ms: number } | { ok: false; kind: string; message: string }

function ApiSection() {
  const conn = useStore(connectionStore)
  const status = useStore(connectionStatus)
  const [baseUrl, setBaseUrl] = useState(conn.baseUrl)
  const [apiKey, setApiKey] = useState(conn.apiKey)
  const [show, setShow] = useState(false)
  const [testing, setTesting] = useState(false)
  const [result, setResult] = useState<TestResult | null>(null)
  const [urlError, setUrlError] = useState<string | null>(null)
  const toast = useToast()

  const validUrl = (v: string) => {
    try {
      const u = new URL(v)
      return u.protocol === 'http:' || u.protocol === 'https:'
    } catch {
      return false
    }
  }

  const test = async () => {
    if (!validUrl(baseUrl)) return setUrlError('http:// 또는 https://로 시작하는 주소를 입력하세요')
    setUrlError(null)
    setTesting(true)
    const t0 = performance.now()
    try {
      await request('/api/user/settings', { connection: { baseUrl, apiKey } })
      setResult({ ok: true, ms: Math.round(performance.now() - t0) })
    } catch (e) {
      const err = e instanceof ApiError ? e : null
      setResult({
        ok: false,
        kind: err?.kind ?? 'network',
        message:
          err?.kind === 'unauthorized'
            ? 'API 키가 맞지 않아요 (401)'
            : err?.kind === 'network'
              ? '서버에 연결할 수 없어요'
              : (err?.message ?? '연결하지 못했어요'),
      })
    } finally {
      setTesting(false)
    }
  }

  const save = (e: FormEvent) => {
    e.preventDefault()
    if (!validUrl(baseUrl)) return setUrlError('http:// 또는 https://로 시작하는 주소를 입력하세요')
    if (!apiKey.trim()) return
    setUrlError(null)
    connectionStore.set({ baseUrl: baseUrl.trim().replace(/\/+$/, ''), apiKey: apiKey.trim() })
    toast.success('연결 정보를 저장했어요', '모든 데이터를 다시 불러와요')
    // 저장 후 전체 데이터를 새 연결로 다시 받는다
    window.setTimeout(() => window.location.reload(), 400)
  }

  const badge = result
    ? result.ok
      ? { color: 'var(--green)', text: `연결됨 · ${result.ms}ms` }
      : { color: 'var(--red)', text: result.message }
    : status === 'online'
      ? { color: 'var(--green)', text: '연결됨' }
      : status === 'unauthorized'
        ? { color: 'var(--red)', text: 'API 키 오류' }
        : status === 'offline'
          ? { color: 'var(--red)', text: '연결 끊김' }
          : { color: 'var(--text-muted)', text: '확인 전' }

  const dirty = baseUrl !== conn.baseUrl || apiKey !== conn.apiKey

  return (
    <Section
      title="API 연결"
      id="api"
      right={
        <span className={s.status} style={{ color: badge.color }} role="status">
          <span className={s.dot} style={{ background: badge.color }} />
          {badge.text}
        </span>
      }
    >
      <form className={s.body} onSubmit={save} noValidate>
        <Field
          label="서버 주소"
          required
          error={urlError}
          hint={
            baseUrl.replace(/\/+$/, '') !== DEFAULT_BASE_URL
              ? '기본 주소가 아니면 서버에 CORS 설정이 필요해요 (README 참고)'
              : undefined
          }
        >
          <Input
            mono
            value={baseUrl}
            onChange={(e) => {
              setBaseUrl(e.target.value)
              setResult(null)
            }}
            placeholder={DEFAULT_BASE_URL}
          />
        </Field>
        <Field label="X-API-KEY" required error={!apiKey.trim() ? '키를 입력하세요' : undefined}>
          <KeyInput
            value={apiKey}
            show={show}
            onToggle={() => setShow((v) => !v)}
            onChange={(v) => {
              setApiKey(v)
              setResult(null)
            }}
          />
        </Field>
        <span className={s.note}>
          서버는 필요할 때만 로컬에서 켜요. 꺼져 있으면 화면 위에 "서버에 연결할 수 없어요" 배너가 떠요.
        </span>
        <div className={s.actions}>
          {(baseUrl !== DEFAULT_BASE_URL || apiKey !== DEFAULT_API_KEY) && (
            <Button
              variant="ghost"
              onClick={() => {
                setBaseUrl(DEFAULT_BASE_URL)
                setApiKey(DEFAULT_API_KEY)
                setResult(null)
              }}
            >
              로컬 기본값
            </Button>
          )}
          <Button onClick={() => void test()} disabled={testing}>
            {testing ? '테스트 중…' : '연결 테스트'}
          </Button>
          <Button type="submit" variant="primary" disabled={!dirty || !apiKey.trim()}>
            저장
          </Button>
        </div>
      </form>
    </Section>
  )
}

/** Field가 id를 넘겨 주도록 input 한 개를 감싼 키 입력 */
function KeyInput({
  value,
  show,
  onToggle,
  onChange,
  ...rest
}: {
  value: string
  show: boolean
  onToggle: () => void
  onChange: (v: string) => void
  id?: string
}) {
  return (
    <div className={s.keyRow}>
      <Input
        {...rest}
        mono
        type={show ? 'text' : 'password'}
        autoComplete="off"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        style={{ flexGrow: 1 }}
      />
      <Button onClick={onToggle} icon={show ? <EyeOff size={13} /> : <Eye size={13} />} aria-pressed={show}>
        {show ? '가리기' : '보기'}
      </Button>
    </div>
  )
}

// ---- 주간 통계 다시 계산 ----

function BatchSection() {
  const today = useToday()
  // 서버 기본값과 같이 "지난주 월요일" (끝난 주를 계산)
  const [week, setWeek] = useState<LocalDate>(() => shiftDate(weekStartOf(today), -7))
  const monday = week ? weekStartOf(week) : ''
  return (
    <Section
      title="주간 통계 다시 계산"
      right={
        <label className={s.weekInput}>
          기준 주
          <Input type="date" mono value={week} onChange={(e) => setWeek(e.target.value)} max={today} />
        </label>
      }
    >
      {week && monday !== week && (
        <div className={s.foot} style={{ borderTop: 0 }}>
          월요일 기준으로 계산해요 → <span className="mono">{monday}</span>
        </div>
      )}
      {WEEKLY_STATS.filter((stat) => stat.key !== 'assignment').map((stat) => (
        <JobRow key={stat.key} stat={stat} weekStart={monday} />
      ))}
      <div className={s.foot}>POST …/batch-runs 응답의 jobExecutionId · status를 옆에 보여줘요.</div>
    </Section>
  )
}

const JOB_NAME: Record<string, string> = {
  meal: '식단 주간 통계',
  health: '체중 · 수면 주간 통계',
  study: '공부 주제별 주간 통계',
  habit: '습관 주간 통계',
  assignment: '과제 주간 통계',
}

function JobRow({ stat, weekStart }: { stat: WeeklyStat<unknown>; weekStart: LocalDate }) {
  const run = useRunWeeklyStat(stat)
  const [last, setLast] = useState<BatchRunResult | null>(null)
  const status = run.isPending ? 'RUNNING' : run.isError ? 'FAILED' : last?.status
  const tone = !status ? null : status === 'COMPLETED' ? 'green' : /FAIL|ABANDON|STOP/.test(status) ? 'red' : 'yellow'
  return (
    <div className={s.job}>
      <div className={s.jobName}>
        <span>{JOB_NAME[stat.key] ?? stat.label}</span>
        <span>{stat.path}</span>
      </div>
      {status && tone ? (
        <Tag tone={tone} mono title={run.error instanceof Error ? run.error.message : undefined}>
          {last && !run.isPending && !run.isError ? `#${last.jobExecutionId} ${last.status}` : status}
        </Tag>
      ) : (
        <span />
      )}
      <Button
        size="sm"
        disabled={!weekStart || run.isPending}
        onClick={() => run.mutate(weekStart, { onSuccess: (r) => setLast(r) })}
      >
        실행
      </Button>
    </div>
  )
}
