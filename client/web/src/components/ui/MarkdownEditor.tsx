import { lazy, Suspense } from 'react'
import s from './Markdown.module.css'

export interface MarkdownEditorProps {
  value: string
  onChange: (markdown: string) => void
  placeholder?: string
  label: string
  className?: string
  autoFocus?: boolean
}

// 편집기 라이브러리(TipTap)가 커서 메모 창을 열 때만 불러온다
const Impl = lazy(() => import('./MarkdownEditorImpl'))

/** 노션처럼 쓰면서 바로 보이는 마크다운 편집기 (이슈 #150). 구현은 MarkdownEditorImpl */
export function MarkdownEditor(props: MarkdownEditorProps) {
  return (
    <Suspense fallback={<div className={`${s.md} ${s.editor}`} aria-busy="true" />}>
      <Impl {...props} />
    </Suspense>
  )
}
