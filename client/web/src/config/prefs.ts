import { createStore } from '../lib/storage'

export const ACCENTS = [
  { key: 'coral', label: '연빨강', swatch: '#e27a72' },
  { key: 'green', label: '초록', swatch: '#6cc28f' },
  { key: 'blue', label: '파랑', swatch: '#6ab0e0' },
  { key: 'purple', label: '보라', swatch: '#b48be0' },
  { key: 'orange', label: '주황', swatch: '#e0995f' },
] as const

export type Accent = (typeof ACCENTS)[number]['key']

/** 강조색은 서버에 저장할 곳이 없어 localStorage에만 둔다 */
export const accentStore = createStore<Accent>('hubdash.accent', 'coral')

/** 허브 링크 고정 여부 (서버에 필드 없음) */
export const pinnedLinksStore = createStore<number[]>('hubdash.pinnedLinks', [])

/** 생활 화면 상단 "학기 다짐" 태그 */
export const pledgesStore = createStore<string[]>('hubdash.pledges', [])

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
