import type { HTMLAttributes, ReactNode } from 'react'
import s from './Card.module.css'
import { cx } from './cx'

interface CardProps extends HTMLAttributes<HTMLElement> {
  /** 안쪽 여백 12/14 + 세로 간격. 목록형(헤더 줄 subtle)이면 false */
  padded?: boolean
  surface?: boolean
  as?: 'section' | 'div' | 'article'
}

export function Card({ padded = true, surface, as: Tag = 'section', className, ...rest }: CardProps) {
  return <Tag className={cx(s.card, padded && s.padded, surface && s.surface, className)} {...rest} />
}

interface SectionHeaderProps {
  title: ReactNode
  count?: ReactNode
  meta?: ReactNode
  actions?: ReactNode
  /** 목록형 카드의 헤더 줄 (--subtle 배경) */
  list?: boolean
  level?: 2 | 3
  id?: string
}

export function SectionHeader({ title, count, meta, actions, list, level = 2, id }: SectionHeaderProps) {
  const H = level === 2 ? 'h2' : 'h3'
  return (
    <div className={cx(s.header, list && s.listHeader)}>
      <H className={s.title} id={id}>
        {title}
      </H>
      {count !== undefined && <span className={s.count}>{count}</span>}
      {meta && <span className={s.meta}>{meta}</span>}
      {actions && <div className={s.actions}>{actions}</div>}
    </div>
  )
}

/** 카드 안 목록 한 줄 (위쪽 구분선) */
export function Row({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cx(s.row, className)} {...rest} />
}
