// 메모 카드 한 줄 미리보기. 본문 편집·표시는 MarkdownEditor(이슈 #150)

/** 목록 미리보기 한 줄: 마크다운 기호를 걷어낸 첫 내용 */
export function plainSnippet(src: string, max = 80): string {
  const text = src
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/^#{1,3}\s+/gm, '')
    .replace(/^\s*([-*]|\d+[.)])\s+(\[[ xX]\]\s+)?/gm, '')
    .replace(/^>\s?/gm, '')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/(?<!\\)[*`]/g, '')
    // 편집기가 저장할 때 넣은 이스케이프 (이슈 #150)
    .replace(/\\([\\`*_~[\]#>])/g, '$1')
    .replace(/&lt;/g, '<')
    .replace(/&amp;/g, '&')
    .replace(/\s+/g, ' ')
    .trim()
  return text.length > max ? `${text.slice(0, max)}…` : text
}
