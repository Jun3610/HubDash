import { useState, type ReactNode } from 'react'
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
      Less
      {[0, 1, 2, 3, 4].map((l) => (
        <HeatCellBox key={l} level={l as HeatLevel} />
      ))}
      More
    </div>
  )
}

/** 최근 1년(53주 × 7) 기록 히트맵 */
export function YearHeatmap({
  counts,
  end,
  label,
  weeks = 53,
  cell = 10,
  labels = true,
}: {
  counts: Map<LocalDate, number>
  end: LocalDate
  label: string
  weeks?: number
  cell?: number
  labels?: boolean
}) {
  const grid = buildYearGrid(counts, end, weeks)
  const months = monthLabels(grid)
  const size = { gridTemplateRows: `repeat(7, ${cell}px)`, gridAutoColumns: `${cell}px` }
  if (!labels) {
    return (
      <div className={s.heatGrid} style={{ ...size, justifyContent: 'space-between' }} role="img" aria-label={label}>
        {grid.flat().map((c) => (
          <HeatCellBox
            key={c.date}
            level={c.level}
            future={c.future}
            size={cell}
            title={c.future ? undefined : `${c.date} · ${c.count}건`}
          />
        ))}
      </div>
    )
  }
  return (
    <div className={s.heatWrap}>
      <div className={s.heatDays} aria-hidden="true">
        <span />
        <span>Mon</span>
        <span />
        <span>Wed</span>
        <span />
        <span>Fri</span>
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

export interface StackDatum {
  key: string
  parts: { value: number; color: BarColor; label: string }[]
  title?: string
}

/** 누적 세로 막대 (날짜별 탄수·지방·단백질 칼로리 등). goal이 있으면 점선 */
export function StackedBarChart({
  data,
  height = 140,
  goal,
  goalLabel,
  axis,
  label,
}: {
  data: StackDatum[]
  height?: number
  goal?: number | null
  goalLabel?: string
  axis?: [ReactNode, ReactNode]
  label: string
}) {
  const totals = data.map((d) => d.parts.reduce((a, p) => a + p.value, 0))
  const top = Math.max(goal ? goal * 1.2 : 0, ...totals, 1)
  return (
    <div>
      <div className={s.bars} style={{ height, gap: 4 }} role="img" aria-label={label}>
        {goal ? (
          <>
            <div className={s.goalLine} style={{ bottom: `${(goal / top) * 100}%` }} />
            <span className={s.goalLabel} style={{ bottom: `${(goal / top) * 100}%` }}>
              {goalLabel ?? goal.toLocaleString()}
            </span>
          </>
        ) : null}
        {data.map((d, i) => (
          <div key={d.key} className={s.barCol} title={d.title}>
            <div className={s.stack} style={{ height: `${(totals[i] / top) * 100}%` }}>
              {d.parts.map((p) =>
                p.value > 0 ? <div key={p.label} style={{ flexGrow: p.value, background: barVar(p.color) }} /> : null,
              )}
            </div>
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

/** 선 그래프 (체중 추이 등). 값이 없는 칸은 건너뛰고, 최솟값·최댓값을 왼쪽에 표시 */
export function LineChart({
  points,
  height = 120,
  color = 'green',
  label,
  unit = '',
  digits = 1,
  axis,
  ticks,
}: {
  points: { key: string; value: number | null; title?: string }[]
  height?: number
  color?: BarColor
  label: string
  unit?: string
  digits?: number
  axis?: [ReactNode, ReactNode]
  /** 점마다 가로축 라벨 (빈 문자열이면 표시 안 함). 있으면 axis 대신 쓴다 (이슈 #218) */
  ticks?: string[]
}) {
  // 커서가 가까운 점: 세로 안내선과 날짜·값 말풍선 (이슈 #218)
  const [hover, setHover] = useState<number | null>(null)
  const vals = points.map((p) => p.value).filter((v): v is number => v !== null)
  if (vals.length === 0) {
    return <div className={s.lineEmpty}>{label} — 기록이 없어요</div>
  }
  const min = Math.min(...vals)
  const max = Math.max(...vals)
  const pad = (max - min || 1) * 0.15
  const lo = min - pad
  const hi = max + pad
  const w = 100
  const n = Math.max(points.length - 1, 1)
  const xy = points
    .map((p, i) => (p.value === null ? null : { x: (i / n) * w, y: ((hi - p.value) / (hi - lo)) * 100, p }))
    .filter((v): v is { x: number; y: number; p: (typeof points)[number] } => v !== null)
  return (
    <div className={s.line}>
      <div className={s.lineScale} aria-hidden="true">
        <span>
          {max.toFixed(digits)}
          {unit}
        </span>
        <span>
          {min.toFixed(digits)}
          {unit}
        </span>
      </div>
      <div style={{ flexGrow: 1, minWidth: 0 }}>
        <div
          className={s.linePlot}
          style={{ height }}
          role="img"
          aria-label={label}
          onMouseLeave={() => setHover(null)}
          onMouseMove={(e) => {
            const r = e.currentTarget.getBoundingClientRect()
            const x = ((e.clientX - r.left) / r.width) * w
            let best: number | null = null
            for (let i = 0; i < xy.length; i++)
              if (best === null || Math.abs(xy[i].x - x) < Math.abs(xy[best].x - x)) best = i
            setHover(best)
          }}
        >
          <svg viewBox="0 0 100 100" preserveAspectRatio="none" width="100%" height="100%">
            <polyline
              points={xy.map((c) => `${c.x},${c.y}`).join(' ')}
              fill="none"
              stroke={barVar(color)}
              strokeWidth={2}
              vectorEffect="non-scaling-stroke"
              strokeLinejoin="round"
            />
          </svg>
          {xy.map((c) => (
            <span
              key={c.p.key}
              className={s.lineDot}
              style={{ left: `${c.x}%`, top: `${c.y}%`, background: barVar(color) }}
              data-hover={hover !== null && xy[hover] === c}
            />
          ))}
          {hover !== null && xy[hover] && (
            <>
              <span className={s.lineGuide} style={{ left: `${xy[hover].x}%` }} aria-hidden="true" />
              <span
                className={s.lineTip}
                data-flip={xy[hover].x > 70}
                style={{ left: `${xy[hover].x}%`, top: `${xy[hover].y}%` }}
              >
                {xy[hover].p.title ?? `${xy[hover].p.key} · ${xy[hover].p.value}${unit}`}
              </span>
            </>
          )}
        </div>
        {ticks ? (
          <div className={s.lineTicks} aria-hidden="true">
            {ticks.map((t, i) =>
              t ? (
                <span key={i} style={{ left: `${(i / n) * 100}%` }}>
                  {t}
                </span>
              ) : null,
            )}
          </div>
        ) : (
          axis && (
            <div className={s.barAxis}>
              <span>{axis[0]}</span>
              <span>{axis[1]}</span>
            </div>
          )
        )}
      </div>
    </div>
  )
}
