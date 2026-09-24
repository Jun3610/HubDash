import { TaskItem, TaskList } from '@tiptap/extension-list'
import { Placeholder } from '@tiptap/extensions'
import { Markdown } from '@tiptap/markdown'
import StarterKit from '@tiptap/starter-kit'
import { escapeMarkdownText } from '../../lib/mdEscape'

type Manager = {
  encodeTextForMarkdown: (text: string, node: unknown, parent?: unknown) => string
  codeTypes: Set<string>
}

/** 저장할 때 이스케이프를 필요한 곳에만 (mdEscape 참고) */
const MemoMarkdown = Markdown.extend({
  onBeforeCreate(props) {
    this.parent?.(props)
    const manager = this.editor.markdown as unknown as Manager
    manager.encodeTextForMarkdown = (text, node, parent) => {
      const n = node as { marks?: ({ type: string } | string)[] }
      const p = parent as { type?: string } | undefined
      const inCode =
        (p?.type != null && manager.codeTypes.has(p.type)) ||
        (n.marks ?? []).some((m) => manager.codeTypes.has(typeof m === 'string' ? m : m.type))
      return inCode ? text : escapeMarkdownText(text)
    }
  },
})

/** 메모 · 과목 메모 에디터 확장 묶음. 테스트에서도 같은 걸 쓴다 */
export function markdownExtensions(placeholder = '') {
  return [
    StarterKit.configure({ link: { openOnClick: true, autolink: true } }),
    TaskList,
    TaskItem.configure({ nested: true }),
    Placeholder.configure({ placeholder }),
    MemoMarkdown,
  ]
}
