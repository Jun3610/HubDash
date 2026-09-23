import type { ReactNode } from 'react'
import { buildYearGrid, monthLabels, type HeatLevel } from '../../lib/heatmap'
import type { LocalDate } from '../../lib/date'
import { ProgressBar } from './Progress'
import { barVar, type BarColor } from './tone'
import s from './Charts.module.css'
import { cx } from './cx'

const LEVEL_CLASS = [s.l0, s.l1, s.l2, s.l3, s.l4]

export function HeatCellBox({
  level,
  future,
  title,
  size,
}: {
  level: HeatLevel
  future?: boolean
  title?: string
  size?: number
}) {
  return (
    <span
      className={cx(s.cell, LEVEL_CLASS[level], future && s.future)}
      title={title}
      style={size ? { width: size, height: size } : undefined}
    />
  )
}

export function HeatLegend() {
  return (
    <div className={s.legend} aria-hidden="true">
      적음
      {[0, 1, 2, 3, 4].map((l) => (
        <HeatCellBox key={l} level={l as HeatLevel} />
      ))}
      많음
    </div>
  )
}

/** 최근 1년(53주 × 7) 기록 히트맵 */
export function YearHeatmap({ counts, end, label }: { counts: Map<LocalDate, number>; end: LocalDate; label: string }) {
  const grid = buildYearGrid(counts, end)
  const months = monthLabels(grid)
  return (
    <div className={s.heatWrap}>
      <div className={s.heatDays} aria-hidden="true">
        <span />
        <span>월</span>
        <span />
        <span>수</span>
        <span />
        <span>금</span>
        <span />
      </div>
      <div className={s.heatBody}>
        <div className={s.heatMonths} aria-hidden="true">
          {months.map((m, i) => (
            <span key={i}>{m}</span>
          ))}
        </div>
        <div className={s.heatGrid} role="img" aria-label={label}>
          {grid.flat().map((c) => (
            <HeatCellBox
              key={c.date}
              level={c.level}
              future={c.future}
              title={c.future ? undefined : `${c.date} · ${c.count}건`}
            />
          ))}
        </div>
      </div>
    </div>
  )
}

export interface BarDatum {
  key: string
  value: number
  label?: string // title 툴팁
  color?: BarColor
}

/** 세로 막대 차트. goal이 있으면 점선 목표선 */
export function BarChart({
  data,
  height = 110,
  max,
  goal,
  goalLabel,
  axis,
  label,
  color = 'blue',
}: {
  data: BarDatum[]
  height?: number
  max?: number
  goal?: number
  goalLabel?: string
  axis?: [ReactNode, ReactNode]
  label: string
  color?: BarColor
}) {
  const top = Math.max(max ?? 0, goal ? goal * 1.33 : 0, ...data.map((d) => d.value), 1)
  return (
    <div>
      <div className={s.bars} style={{ height }} role="img" aria-label={label}>
        {goal !== undefined && (
          <>
            <div className={s.goalLine} style={{ bottom: `${(goal / top) * 100}%` }} />
            <span className={s.goalLabel} style={{ bottom: `${(goal / top) * 100}%` }}>
              {goalLabel ?? goal.toLocaleString()}
            </span>
          </>
        )}
        {data.map((d) => (
          <div key={d.key} className={s.barCol} title={d.label}>
            <div
              className={s.bar}
              style={{ height: `${(d.value / top) * 100}%`, background: barVar(d.color ?? color) }}
            />
          </div>
        ))}
      </div>
      {axis && (
        <div className={s.barAxis}>
          <span>{axis[0]}</span>
          <span>{axis[1]}</span>
        </div>
      )}
    </div>
  )
}

/** 가로 막대 목록 (주제별 시간 등) */
export function HBarList({
  rows,
  color = 'purple',
}: {
  rows: { key: string; label: ReactNode; value: number; display: ReactNode; color?: BarColor }[]
  color?: BarColor
}) {
  const max = Math.max(1, ...rows.map((r) => r.value))
  return (
    <div>
      {rows.map((r) => (
        <div key={r.key} className={s.hbarRow}>
          <span className="ellipsis">{r.label}</span>
          <ProgressBar value={(r.value / max) * 100} color={r.color ?? color} />
          <span className={s.hbarValue}>{r.display}</span>
        </div>
      ))}
    </div>
  )
}

/** 작은 선 그래프. null 값은 건너뛴다 */
export function Sparkline({
  values,
  height = 28,
  color = 'blue',
  label,
}: {
  values: (number | null)[]
  height?: number
  color?: BarColor
  label: string
}) {
  const pts = values.map((v, i) => ({ v, i })).filter((p): p is { v: number; i: number } => p.v !== null)
  const w = 100
  if (pts.length === 0) {
    return <svg className={s.spark} height={height} role="img" aria-label={`${label} — 기록 없음`} />
  }
  const min = Math.min(...pts.map((p) => p.v))
  const max = Math.max(...pts.map((p) => p.v))
  const span = max - min || 1
  const n = Math.max(values.length - 1, 1)
  const coords = pts.map((p) => [(p.i / n) * w, height - 3 - ((p.v - min) / span) * (height - 6)] as const)
  return (
    <svg
      className={s.spark}
      viewBox={`0 0 ${w} ${height}`}
      preserveAspectRatio="none"
      height={height}
      role="img"
      aria-label={label}
    >
      <polyline
        points={coords.map((c) => c.join(',')).join(' ')}
        fill="none"
        stroke={barVar(color)}
        strokeWidth={1.6}
        vectorEffect="non-scaling-stroke"
        strokeLinejoin="round"
        strokeLinecap="round"
      />
    </svg>
  )
}
