import { createStore } from '../lib/storage'

export const ACCENTS = [
  { key: 'coral', label: '연빨강', swatch: '#e27a72' },
  { key: 'green', label: '초록', swatch: '#529e72' },
  { key: 'blue', label: '파랑', swatch: '#529cca' },
  { key: 'purple', label: '보라', swatch: '#9d68d3' },
  { key: 'orange', label: '주황', swatch: '#c77d48' },
] as const

export type Accent = (typeof ACCENTS)[number]['key']

/** 강조색은 서버에 저장할 곳이 없어 localStorage에만 둔다 */
export const accentStore = createStore<Accent>('hubdash.accent', 'coral')

/** 허브 링크 고정 여부 (서버에 필드 없음) */
export const pinnedLinksStore = createStore<number[]>('hubdash.pinnedLinks', [])

/** 생활 화면 상단 "학기 다짐" 태그 */
export const pledgesStore = createStore<string[]>('hubdash.pledges', [])

/** 이니셜 아바타 색 ("이니셜 색 바꾸기") — 서버에 필드가 없어 localStorage */
export const AVATAR_COLORS = ['#2383e2', '#c4554d', '#3f7d59', '#7d4fb3', '#a8622f', '#5a6270'] as const
export const avatarColorStore = createStore<string>('hubdash.avatarColor', AVATAR_COLORS[0])

/** 서버 settings.theme 값 (서버 기본값이 "LIGHT"라 대문자로 저장) */
export type ThemeValue = 'DARK' | 'LIGHT' | 'SYSTEM'
export type ThemePref = 'dark' | 'light' | 'system'

/** 서버 settings.theme을 <html data-theme>에 반영 */
export function applyTheme(theme: string | undefined) {
  const pref = (theme ?? 'dark').toLowerCase() as ThemePref
  const resolved =
    pref === 'system'
      ? window.matchMedia?.('(prefers-color-scheme: light)').matches
        ? 'light'
        : 'dark'
      : pref === 'light'
        ? 'light'
        : 'dark'
  document.documentElement.dataset.theme = resolved
}

export function applyAccent(accent: Accent) {
  document.documentElement.dataset.accent = accent
}
