import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Markdown } from '../components/ui/Markdown'
import { parseBlocks, plainSnippet } from './markdown'

describe('parseBlocks', () => {
  it('제목·목록·문단·코드·구분선', () => {
    const b = parseBlocks(
      '# 공부 방향\n\n- CS 기초\n- **백엔드**\n\n1. 이슈\n2. 브랜치\n\n문단 첫 줄\n둘째 줄\n\n```\nconst a = 1\n```\n---',
    )
    expect(b.map((x) => x.t)).toEqual(['h', 'ul', 'ol', 'p', 'code', 'hr'])
    expect(b[1]).toEqual({ t: 'ul', items: [{ text: 'CS 기초' }, { text: '**백엔드**' }] })
    expect(b[3]).toEqual({ t: 'p', text: '문단 첫 줄\n둘째 줄' })
  })
  it('체크박스 목록', () => {
    expect(parseBlocks('- [x] 끝\n- [ ] 남음')).toEqual([
      {
        t: 'ul',
        items: [
          { text: '끝', checked: true },
          { text: '남음', checked: false },
        ],
      },
    ])
  })
})

describe('Markdown 렌더', () => {
  it('인라인 굵게·기울임·코드·링크', () => {
    const { container } = render(<Markdown source="**굵게** *기울임* `code` [링크](https://example.com)" />)
    expect(container.querySelector('strong')?.textContent).toBe('굵게')
    expect(container.querySelector('em')?.textContent).toBe('기울임')
    expect(container.querySelector('code')?.textContent).toBe('code')
    const a = container.querySelector('a')!
    expect(a.getAttribute('href')).toBe('https://example.com')
    expect(a.getAttribute('target')).toBe('_blank')
  })
  it('HTML은 글자로만 보이고 실행되지 않는다', () => {
    const { container } = render(<Markdown source={'<img src=x onerror="alert(1)"> <script>alert(1)</script>'} />)
    expect(container.querySelector('img')).toBeNull()
    expect(container.querySelector('script')).toBeNull()
    expect(container.textContent).toContain('<script>')
  })
  it('javascript: 링크는 링크로 만들지 않는다', () => {
    const { container } = render(<Markdown source="[누르기](javascript:alert(1))" />)
    expect(container.querySelector('a')).toBeNull()
  })
})

describe('plainSnippet', () => {
  it('기호를 걷어낸 한 줄', () => {
    expect(plainSnippet('# 제목\n- **굵게** 항목\n[링크](https://x.y)')).toBe('제목 굵게 항목 링크')
  })
  it('길면 줄임', () => {
    expect(plainSnippet('가'.repeat(100), 10)).toBe(`${'가'.repeat(10)}…`)
  })
})
