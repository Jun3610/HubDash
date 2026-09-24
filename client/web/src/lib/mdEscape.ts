/**
 * 에디터가 마크다운으로 저장할 때 글자를 이스케이프하는 규칙 (이슈 #150).
 * @tiptap/markdown 기본값은 [ ] ~ < > 를 늘 \[ 나 &lt; 로 바꿔서, 노션에서 옮긴 메모
 * ("[식단 1500kcal]", "1g << 2g", "-> 산책")를 한 번 열어 저장하면 원문이 지저분해진다.
 * 여기서는 다시 읽을 때 서식으로 오해될 수 있는 경우에만 이스케이프한다.
 */
const WORD = /[\p{L}\p{N}]/u

export function escapeMarkdownText(text: string): string {
  let out = ''
  for (let i = 0; i < text.length; i++) {
    const c = text[i]
    const prev = text[i - 1] ?? ''
    const next = text[i + 1] ?? ''
    const lineStart = i === 0 || prev === '\n'
    switch (c) {
      case '\\':
      case '`':
      case '*':
        out += '\\' + c
        break
      case '_':
        // 단어 안의 _(snake_case)는 기울임이 되지 않는다
        out += WORD.test(prev) && WORD.test(next) ? c : '\\' + c
        break
      case '~':
        // ~~ 만 취소선
        out += prev === '~' || next === '~' ? '\\' + c : c
        break
      case '[':
        // 줄 맨 앞 [ ] / [x] 는 할 일 목록으로 읽힌다
        out += lineStart && /^\[[ xX]\]/.test(text.slice(i)) ? '\\' + c : c
        break
      case ']':
        // ](주소) 는 링크
        out += next === '(' ? '\\' + c : c
        break
      case '<':
        // <div, </b, <!-- 처럼 HTML 태그로 읽힐 때만
        out += /[A-Za-z/!?]/.test(next) ? '&lt;' : c
        break
      case '&':
        // &amp; 같은 문자 참조로 읽힐 때만
        out += /^&#?\w+;/.test(text.slice(i)) ? '&amp;' : c
        break
      case '>':
      case '#':
        // 줄 맨 앞 > 는 인용, # 는 제목
        out += lineStart ? '\\' + c : c
        break
      default:
        out += c
    }
  }
  return out
}
