import {
  cloneElement,
  forwardRef,
  isValidElement,
  useId,
  type InputHTMLAttributes,
  type ReactElement,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from 'react'
import { ApiError } from '../../api/client'
import s from './Form.module.css'
import { cx } from './cx'

/** label + 입력 + 오류 메시지. 자식 입력에 id·aria 속성을 연결해 준다 */
export function Field({
  label,
  required,
  error,
  hint,
  children,
  className,
}: {
  label: ReactNode
  required?: boolean
  error?: string | null
  hint?: ReactNode
  children: ReactElement<{ id?: string; 'aria-invalid'?: boolean; 'aria-describedby'?: string; required?: boolean }>
  className?: string
}) {
  const id = useId()
  const msgId = `${id}-msg`
  const control = isValidElement(children)
    ? cloneElement(children, {
        id: children.props.id ?? id,
        'aria-invalid': error ? true : undefined,
        'aria-describedby': error || hint ? msgId : undefined,
        required,
      })
    : children
  return (
    <div className={cx(s.field, className)}>
      <label htmlFor={children.props.id ?? id} className={s.label}>
        {label}
        {required && (
          <span className={s.required} aria-hidden="true">
            *
          </span>
        )}
      </label>
      {control}
      {error ? (
        <span id={msgId} className={s.error} role="alert">
          {error}
        </span>
      ) : hint ? (
        <span id={msgId} className={s.hint}>
          {hint}
        </span>
      ) : null}
    </div>
  )
}

type InputProps = InputHTMLAttributes<HTMLInputElement> & { mono?: boolean }

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input({ className, mono, ...rest }, ref) {
  return <input ref={ref} className={cx(s.control, mono && s.monoControl, className)} {...rest} />
})

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>(function Select(
  { className, ...rest },
  ref,
) {
  return <select ref={ref} className={cx(s.control, className)} {...rest} />
})

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(function Textarea(
  { className, ...rest },
  ref,
) {
  return <textarea ref={ref} className={cx(s.control, className)} {...rest} />
})

export function Checkbox({
  checked,
  onChange,
  label,
  hideLabel,
  disabled,
  color,
}: {
  checked: boolean
  onChange: (checked: boolean) => void
  label: ReactNode
  hideLabel?: boolean
  disabled?: boolean
  color?: string
}) {
  return (
    <label className={s.check}>
      <input
        type="checkbox"
        checked={checked}
        disabled={disabled}
        onChange={(e) => onChange(e.target.checked)}
        style={color ? { accentColor: color } : undefined}
      />
      <span className={hideLabel ? 'sr-only' : undefined}>{label}</span>
    </label>
  )
}

export function Switch({
  checked,
  onChange,
  label,
  disabled,
  id,
}: {
  checked: boolean
  onChange: (checked: boolean) => void
  label: string
  disabled?: boolean
  id?: string
}) {
  return (
    <button
      id={id}
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      disabled={disabled}
      className={s.switch}
      onClick={() => onChange(!checked)}
    />
  )
}

/** 폼 하단 서버 오류. 서버 검증 메시지는 필드 이름 없이 오므로 폼 단위로 보여 준다 */
export function FormError({ error }: { error: unknown }) {
  if (!error) return null
  const e = error instanceof ApiError ? error : null
  return (
    <div className={s.formError} role="alert">
      {e?.errorCode && <code>{e.errorCode}</code>}
      <span>{e ? e.message : String((error as Error).message ?? error)}</span>
    </div>
  )
}
