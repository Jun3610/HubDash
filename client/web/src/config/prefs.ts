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

/** 사이드바에 보여 줄 학기 (null이면 오늘 기준 현재 학기, 이슈 #132) */
export const sidebarSemesterStore = createStore<number | null>('hubdash.sidebarSemester', null)

/** 사이드바 메뉴 순서 (메뉴 key 배열, 이슈 #132) */
export const navOrderStore = createStore<string[]>('hubdash.navOrder', [])

/** 카드·입력칸 테두리 색 (이슈 #132) */
export const BORDERS = [
  { key: 'white', label: '은은한 흰색 + 그림자' },
  { key: 'accent', label: '강조색 섞기' },
  { key: 'gray', label: '기존 회색' },
  { key: 'light', label: '밝은 회색' },
] as const
export type BorderStyle = (typeof BORDERS)[number]['key']
// 이슈 #148에서 기본값을 흰색으로 바꾸며 키를 새로 둬서, 예전 선택(강조색)도 흰색에서 다시 시작한다
export const borderStore = createStore<BorderStyle>('hubdash.border.v2', 'white')

export function applyBorder(border: BorderStyle) {
  document.documentElement.dataset.border = border
}

/** 과목 분류(시경/컴공/교양 등) — 서버에 필드가 없어 과목 id별로 localStorage */
export const courseCategoryStore = createStore<Record<number, string>>('hubdash.courseCategory', {})
