import { EditorContent, useEditor } from '@tiptap/react'
import { useEffect, useRef } from 'react'
import { cx } from './cx'
import s from './Markdown.module.css'
import type { MarkdownEditorProps } from './MarkdownEditor'
import { markdownExtensions } from './markdownEditorKit'

/**
 * 노션처럼 쓰면서 바로 보이는 마크다운 편집기 (이슈 #150).
 * "# " → 제목, "- " → 목록, "[] " → 할 일, "**굵게**", "> " → 인용, "```" → 코드.
 * 저장 형식은 마크다운 문자열. 처음 불러온 값은 onChange로 돌려보내지 않아(사용자가 고칠 때만) 열기만 해서는 원문이 바뀌지 않는다.
 */
export default function MarkdownEditorImpl({
  value,
  onChange,
  placeholder,
  label,
  className,
  autoFocus,
}: MarkdownEditorProps) {
  const changeRef = useRef(onChange)
  const baseline = useRef('')
  useEffect(() => {
    changeRef.current = onChange
  })
  const editor = useEditor({
    extensions: markdownExtensions(placeholder),
    content: value,
    contentType: 'markdown',
    autofocus: autoFocus ? 'end' : false,
    immediatelyRender: true,
    // 불러온 직후의 모양을 기억해 둔다. 에디터가 끝에 빈 문단을 붙이는 등 내용이 그대로면 원문을 돌려줘서
    // 열고 누르기만 해도 다시 직렬화한 글(목록 앞 빈 줄 등)이 저장되는 일을 막는다
    onCreate: ({ editor }) => {
      baseline.current = editor.getMarkdown().trimEnd()
    },
    onUpdate: ({ editor }) => {
      const md = editor.getMarkdown().trimEnd()
      changeRef.current(md === baseline.current ? value : md)
    },
    editorProps: {
      attributes: {
        class: cx(s.md, s.editor, className),
        'aria-label': label,
        role: 'textbox',
        'aria-multiline': 'true',
      },
    },
  })
  return <EditorContent editor={editor} />
}
