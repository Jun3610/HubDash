import { describe, expect, it } from 'vitest'
import { plainSnippet } from './markdown'

describe('plainSnippet', () => {
  it('기호를 걷어낸 한 줄', () => {
    expect(plainSnippet('# 제목\n- **굵게** 항목\n[링크](https://x.y)')).toBe('제목 굵게 항목 링크')
  })
  it('편집기 이스케이프는 걷어낸다', () => {
    expect(plainSnippet('\\*별\\* snake\\_x 1 &lt;div> &amp;amp;')).toBe('*별* snake_x 1 <div> &amp;')
  })
  it('길면 줄임', () => {
    expect(plainSnippet('가'.repeat(100), 10)).toBe(`${'가'.repeat(10)}…`)
  })
})
