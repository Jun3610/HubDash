/** "Park Jun Young" → "PY", "박준영" → "준영" (한글 이름은 성을 뺀 두 글자) */
export function initials(name: string | undefined | null): string {
  if (!name?.trim()) return '?'
  const trimmed = name.trim()
  if (/^[가-힣]{3,4}$/.test(trimmed)) return trimmed.slice(-2)
  const parts = trimmed.split(/\s+/)
  if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
  return name.trim().slice(0, 2).toUpperCase()
}

/** 1240 → "1,240" (null이면 "—") */
export function num(value: number | null | undefined, digits = 0): string {
  if (value === null || value === undefined || !Number.isFinite(value)) return '—'
  return value.toLocaleString('ko-KR', { maximumFractionDigits: digits, minimumFractionDigits: 0 })
}

/** 비율(0~100) — 목표가 0이면 0 */
export function pct(value: number, goal: number): number {
  return goal > 0 ? (value / goal) * 100 : 0
}

/** 쉼표 구분 태그 문자열 → 배열 */
export function splitTags(tags: string | null | undefined): string[] {
  return (tags ?? '')
    .split(',')
    .map((t) => t.trim())
    .filter(Boolean)
}

export function joinTags(tags: string[]): string {
  return tags
    .map((t) => t.trim())
    .filter(Boolean)
    .join(',')
}
