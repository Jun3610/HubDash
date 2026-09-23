export type Tone = 'neutral' | 'gray' | 'blue' | 'green' | 'purple' | 'yellow' | 'orange' | 'red' | 'accent'

/** 도메인·태그 문자열에 늘 같은 색을 붙이기 위한 해시 */
const TAG_TONES: Tone[] = ['blue', 'green', 'purple', 'yellow', 'orange', 'red']
export function toneFor(key: string): Tone {
  let h = 0
  for (const ch of key) h = (h * 31 + ch.charCodeAt(0)) | 0
  return TAG_TONES[Math.abs(h) % TAG_TONES.length]
}

export type BarColor = 'accent' | 'blue' | 'green' | 'purple' | 'yellow' | 'orange' | 'red' | 'gray'

export const barVar = (c: BarColor) => (c === 'gray' ? 'var(--text-muted)' : `var(--${c})`)
