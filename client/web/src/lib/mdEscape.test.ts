import { Editor } from '@tiptap/core'
import { afterEach, describe, expect, it } from 'vitest'
import { markdownExtensions } from '../components/ui/markdownEditorKit'
import { escapeMarkdownText } from './mdEscape'

const editors: Editor[] = []
const load = (md: string) => {
  const e = new Editor({ extensions: markdownExtensions(), content: md, contentType: 'markdown' })
  editors.push(e)
  return e
}
const roundTrip = (md: string) => load(md).getMarkdown()
afterEach(() => editors.splice(0).forEach((e) => e.destroy()))

describe('escapeMarkdownText', () => {
  it('서식으로 오해될 때만 이스케이프', () => {
    expect(escapeMarkdownText('[식단 1500kcal] 1g << 2g -> 산책 ~!!')).toBe('[식단 1500kcal] 1g << 2g -> 산책 ~!!')
    expect(escapeMarkdownText('snake_case *별* `틱`')).toBe('snake_case \\*별\\* \\`틱\\`')
    expect(escapeMarkdownText('~~취소~~ [글](주소) <div> &amp;')).toBe(
      '\\~\\~취소\\~\\~ [글\\](주소) &lt;div> &amp;amp;',
    )
    expect(escapeMarkdownText('# 제목 아님\n> 인용 아님')).toBe('\\# 제목 아님\n\\> 인용 아님')
  })
})

describe('마크다운 편집기 왕복', () => {
  it('노션에서 옮긴 메모 같은 글은 그대로 저장된다', () => {
    const md =
      '[주 3일]\n\n1: 어깨 + 하체\n2: 가슴 + 삼두\n\n- 탄수 1g << 쌀 2g -> 산책\n- 이번 학기 롤 안하기~!!\n- 컴공 복전 [2027-1] 끝나고'
    expect(roundTrip(md)).toBe(md)
  })
  it('서식은 유지되고 한 번 더 돌려도 같다', () => {
    const md = '# 제목\n\n**굵게** *기울임* `code` [링크](https://example.com)\n\n- [ ] 할 일\n- [x] 끝\n\n> 인용'
    const once = roundTrip(md)
    expect(roundTrip(once)).toBe(once)
    const e = load(md)
    expect(e.getHTML()).toContain('<strong>굵게</strong>')
    expect(e.getHTML()).toContain('data-type="taskList"')
  })
  it('HTML은 실행되지 않고 javascript: 링크는 만들지 않는다', () => {
    const html = load(
      '<img src=x onerror="alert(1)"> <script>alert(1)</script>\n\n[누르기](javascript:alert(1))',
    ).getHTML()
    expect(html).not.toContain('<img')
    expect(html).not.toContain('<script')
    expect(html).not.toContain('javascript:')
  })
})
