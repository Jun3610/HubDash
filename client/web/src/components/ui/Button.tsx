import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { forwardRef } from 'react'
import s from './Button.module.css'
import { cx } from './cx'

type Variant = 'primary' | 'secondary' | 'danger' | 'link' | 'ghost'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: 'md' | 'sm'
  icon?: ReactNode
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { variant = 'secondary', size = 'md', icon, className, children, type = 'button', ...rest },
  ref,
) {
  return (
    <button ref={ref} type={type} className={cx(s.btn, s[variant], size === 'sm' && s.sm, className)} {...rest}>
      {icon}
      {children}
    </button>
  )
})

export interface IconButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  label: string
  size?: 'md' | 'sm'
}

/** 아이콘만 있는 버튼. label은 aria-label로 들어간다 */
export function IconButton({ label, size = 'md', className, children, type = 'button', ...rest }: IconButtonProps) {
  return (
    <button
      type={type}
      aria-label={label}
      title={label}
      className={cx(s.btn, s.icon, size === 'sm' && s.sm, className)}
      {...rest}
    >
      {children}
    </button>
  )
}
