// 서버 검증 규칙(@NotBlank, @Size, 범위)을 프론트에서 먼저 검사한다. 오류 메시지는 필드 아래에 표시.

export type Errors<K extends string = string> = Partial<Record<K, string>>

/** 받침이 있으면 "을", 없으면 "를" (한글이 아니면 "을(를)") */
export function eulReul(word: string): string {
  const code = word.charCodeAt(word.length - 1)
  if (code < 0xac00 || code > 0xd7a3) return '을(를)'
  return (code - 0xac00) % 28 ? '을' : '를'
}

export function required(v: unknown, label = '값'): string | undefined {
  if (v === null || v === undefined || (typeof v === 'string' && !v.trim()))
    return `${label}${eulReul(label)} 입력하세요`
}

export function maxLen(v: string | null | undefined, max: number): string | undefined {
  if (v && v.length > max) return `${max}자 이하로 입력하세요 (지금 ${v.length}자)`
}

/** 빈 값은 통과 (선택 필드), 숫자가 아니거나 범위를 벗어나면 오류 */
export function numRange(v: string, min: number, max: number, integer = false): string | undefined {
  if (v.trim() === '') return
  const n = Number(v)
  if (!Number.isFinite(n)) return '숫자를 입력하세요'
  if (integer && !Number.isInteger(n)) return '정수로 입력하세요'
  if (n < min || n > max) return `${min}~${max} 사이로 입력하세요`
}

export function isUrl(v: string): string | undefined {
  try {
    const u = new URL(v)
    if (u.protocol !== 'http:' && u.protocol !== 'https:') return 'http:// 또는 https://로 시작해야 해요'
  } catch {
    return '주소 형식이 아니에요 (https://…)'
  }
}

/** "" → null, "12.5" → 12.5 */
export function optNum(v: string): number | null {
  return v.trim() === '' ? null : Number(v)
}

export function optStr(v: string): string | null {
  return v.trim() === '' ? null : v.trim()
}

/** 첫 오류가 있으면 true */
export function hasErrors(e: Errors): boolean {
  return Object.values(e).some(Boolean)
}
