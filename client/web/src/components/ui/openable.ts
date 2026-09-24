import type { KeyboardEvent, MouseEvent } from 'react'
import s from './Openable.module.css'

/**
 * 카드 전체를 눌러 작은 창을 여는 속성 (이슈 #148). 카드 안의 버튼·링크·입력은 제 일만 한다.
 * <Card {...openable('식단 기록 열기', open)} />처럼 펼쳐 쓴다. className을 따로 줄 때는 openClass를 같이 붙인다.
 */
export function openable(label: string, onOpen: () => void) {
  const inner = (e: MouseEvent | KeyboardEvent) =>
    (e.target as HTMLElement).closest('button, a, input, select, textarea, label') !== null &&
    e.target !== e.currentTarget
  return {
    role: 'button',
    tabIndex: 0,
    'aria-label': label,
    className: s.openable,
    onClick: (e: MouseEvent) => {
      if (!inner(e)) onOpen()
    },
    onKeyDown: (e: KeyboardEvent) => {
      // ⌘/Ctrl + Enter는 빠른 기록 단축키라 카드 열기로 쓰지 않는다
      if ((e.key === 'Enter' || e.key === ' ') && !e.metaKey && !e.ctrlKey && e.target === e.currentTarget) {
        e.preventDefault()
        onOpen()
      }
    },
  }
}

export const openClass = s.openable
