import { createStore } from '../lib/storage'

// 목표값은 서버에 저장할 곳이 없어 프론트 상수 + localStorage로 둔다.
export interface Goals {
  calories: number // 목표 kcal
  proteinG: number // 이상
  carbsG: number // 이하
  fatG: number // 이하
  sodiumMg: number // 대 (이 값을 넘으면 경고)
  studyWeekMinutes: number // 주간 공부 목표 (분)
}

export const DEFAULT_GOALS: Goals = {
  calories: 1500,
  proteinG: 160,
  carbsG: 140,
  fatG: 50,
  sodiumMg: 2000,
  studyWeekMinutes: 600,
}

const stored = createStore<Goals>('hubdash.goals', DEFAULT_GOALS)
// 새 목표 항목이 생겨도 예전에 저장된 값과 합쳐 빈 값이 없게 한다
stored.set((g) => ({ ...DEFAULT_GOALS, ...g }))
export const goalsStore = stored
