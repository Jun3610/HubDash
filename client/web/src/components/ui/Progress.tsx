import type { ReactNode } from 'react'
import s from './Progress.module.css'
import { barVar, type BarColor } from './tone'
import { cx } from './cx'

/** 0~100 진행 막대. over=true면(목표 초과) 빨강 */
export function ProgressBar({
  value,
  color = 'accent',
  over,
  thin,
  label,
}: {
  value: number
  color?: BarColor
  over?: boolean
  thin?: boolean
  label?: string
}) {
  const pct = Math.max(0, Math.min(100, Number.isFinite(value) ? value : 0))
  return (
    <div
      className={cx(s.track, thin && s.thin)}
      role="progressbar"
      aria-label={label}
      aria-valuenow={Math.round(pct)}
      aria-valuemin={0}
      aria-valuemax={100}
    >
      <div className={s.fill} style={{ width: `${pct}%`, background: barVar(over ? 'red' : color) }} />
    </div>
  )
}

export function KpiTile({
  label,
  value,
  unit,
  progress,
  color = 'blue',
  over,
}: {
  label: ReactNode
  value: ReactNode
  unit?: ReactNode
  progress?: number
  color?: BarColor
  over?: boolean
}) {
  return (
    <div className={s.kpi}>
      <span className={s.kpiLabel}>{label}</span>
      <div className={s.kpiValueRow}>
        <span className={s.kpiValue}>{value}</span>
        {unit && <span className={s.kpiUnit}>{unit}</span>}
      </div>
      {progress !== undefined && <ProgressBar value={progress} color={color} over={over} thin />}
    </div>
  )
}
