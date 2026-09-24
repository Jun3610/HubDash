import type { DietGoal, MealRecord, MealType } from '../../api/types'
import { datePart, shiftDate, type LocalDate } from '../date'

/** 표시·입력 순서: 지방 → 탄수 → 단백질 → 칼로리 (사용자 요청, 이슈 #131 → #148에서 지방을 앞으로) */
export const MACROS = [
  { key: 'fatG', label: 'Fat', unit: 'g', kcalPerG: 9, color: 'red' }, // 탄수(노랑)와 구분되게 빨강 (이슈 #203)
  { key: 'carbsG', label: 'Carbs', unit: 'g', kcalPerG: 4, color: 'yellow' },
  { key: 'proteinG', label: 'Protein', unit: 'g', kcalPerG: 4, color: 'green' },
] as const
export type MacroKey = (typeof MACROS)[number]['key']

export interface Macros {
  carbsG: number
  fatG: number
  proteinG: number
}

/** 칼로리는 입력하지 않고 탄수×4 + 지방×9 + 단백질×4로 계산 (반올림) */
export function kcalOf(m: Macros): number {
  return Math.round(m.carbsG * 4 + m.fatG * 9 + m.proteinG * 4)
}

export interface MealEntry extends Macros {
  type: MealType
  title: string
  kcal: number
  /** 이 끼니를 이루는 서버 기록들 (보통 1건. 예전 방식으로 음식을 여러 개 넣은 기록은 합쳐서 보여 준다) */
  records: MealRecord[]
}

/** 그날 끼니 기록을 끼니 종류별로 합친다. 기록이 없으면 그 끼니는 결과에 없다 */
export function mealsOn(records: MealRecord[], date: LocalDate): Partial<Record<MealType, MealEntry>> {
  const out: Partial<Record<MealType, MealEntry>> = {}
  for (const r of records) {
    if (datePart(r.consumedAt) !== date) continue
    const cur = out[r.mealType] ?? {
      type: r.mealType,
      title: '',
      carbsG: 0,
      fatG: 0,
      proteinG: 0,
      kcal: 0,
      records: [],
    }
    cur.records.push(r)
    for (const it of r.items) {
      cur.carbsG += it.carbsG ?? 0
      cur.fatG += it.fatG ?? 0
      cur.proteinG += it.proteinG ?? 0
    }
    const names = r.items.map((i) => i.name).filter(Boolean)
    cur.title = [cur.title, ...names].filter(Boolean).join(', ')
    out[r.mealType] = cur
  }
  for (const m of Object.values(out)) m!.kcal = kcalOf(m!)
  return out
}

export interface DayTotals extends Macros {
  date: LocalDate
  kcal: number
  meals: number
}

/** 최근 n일(오래된 → 오늘) 날짜별 탄단지·칼로리 합계 — 그래프용 */
export function dailySeries(records: MealRecord[], today: LocalDate, n: number): DayTotals[] {
  const days = Array.from({ length: n }, (_, i) => shiftDate(today, i - n + 1))
  const by = new Map<LocalDate, DayTotals>(
    days.map((d) => [d, { date: d, carbsG: 0, fatG: 0, proteinG: 0, kcal: 0, meals: 0 }]),
  )
  for (const r of records) {
    const t = by.get(datePart(r.consumedAt))
    if (!t) continue
    t.meals++
    for (const it of r.items) {
      t.carbsG += it.carbsG ?? 0
      t.fatG += it.fatG ?? 0
      t.proteinG += it.proteinG ?? 0
    }
  }
  return days.map((d) => {
    const t = by.get(d)!
    return { ...t, kcal: kcalOf(t) }
  })
}

export type GoalRule = 'AT_MOST' | 'AT_LEAST'

/** 목표 대비 상태: 목표 없음 / 달성 / 미달(이상 목표) / 초과(이하 목표) */
export function goalState(
  value: number,
  goal: number | null | undefined,
  rule: GoalRule,
): 'none' | 'ok' | 'under' | 'over' {
  if (goal === null || goal === undefined) return 'none'
  if (rule === 'AT_MOST') return value > goal ? 'over' : 'ok'
  return value >= goal ? 'ok' : 'under'
}

/** 칼로리 중 탄단지 비율(%) — 합이 0이면 모두 0 */
export function macroShare(m: Macros): Record<MacroKey, number> {
  const k = { carbsG: m.carbsG * 4, fatG: m.fatG * 9, proteinG: m.proteinG * 4 }
  const total = k.carbsG + k.fatG + k.proteinG
  if (!total) return { carbsG: 0, fatG: 0, proteinG: 0 }
  return {
    carbsG: Math.round((k.carbsG / total) * 100),
    fatG: Math.round((k.fatG / total) * 100),
    proteinG: Math.round((k.proteinG / total) * 100),
  }
}

/** "140g 이하" / "목표 없음" */
export function ruleText(goal: number | null, rule: GoalRule, unit: string): string {
  if (goal === null) return 'No goal'
  return `${rule === 'AT_MOST' ? 'max' : 'min'} ${goal.toLocaleString('ko-KR')}${unit}`
}

export interface DietRow {
  key: 'carbsG' | 'fatG' | 'proteinG' | 'calories'
  label: string
  unit: string
  value: number
  goal: number | null
  rule: GoalRule
  color: 'red' | 'yellow' | 'orange' | 'green' | 'blue'
}

/** 목표 대비 표시 줄: 지방 → 탄수 → 단백질 → 칼로리 (목표가 없으면 goal=null) */
export function dietRows(goal: DietGoal | undefined, t: Macros & { kcal: number }): DietRow[] {
  return [
    {
      key: 'fatG',
      label: 'Fat',
      unit: 'g',
      value: t.fatG,
      goal: goal?.fatG ?? null,
      rule: goal?.fatRule ?? 'AT_MOST',
      color: 'red',
    },
    {
      key: 'carbsG',
      label: 'Carbs',
      unit: 'g',
      value: t.carbsG,
      goal: goal?.carbsG ?? null,
      rule: goal?.carbsRule ?? 'AT_MOST',
      color: 'yellow',
    },
    {
      key: 'proteinG',
      label: 'Protein',
      unit: 'g',
      value: t.proteinG,
      goal: goal?.proteinG ?? null,
      rule: goal?.proteinRule ?? 'AT_LEAST',
      color: 'green',
    },
    {
      key: 'calories',
      label: 'Calories',
      unit: 'kcal',
      value: t.kcal,
      goal: goal?.calories ?? null,
      rule: goal?.caloriesRule ?? 'AT_MOST',
      color: 'blue',
    },
  ]
}

/** 그날 전체 합계 (끼니 기록의 탄단지 합, 칼로리는 계산) */
export function dayTotals(records: MealRecord[], date: LocalDate): Macros & { kcal: number; meals: number } {
  const meals = Object.values(mealsOn(records, date))
  const t = meals.reduce(
    (a, m) => ({ carbsG: a.carbsG + m.carbsG, fatG: a.fatG + m.fatG, proteinG: a.proteinG + m.proteinG }),
    { carbsG: 0, fatG: 0, proteinG: 0 },
  )
  return { ...t, kcal: kcalOf(t), meals: meals.length }
}
