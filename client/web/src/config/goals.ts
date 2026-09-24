import { createStore } from '../lib/storage'

// 식단 목표는 서버(/api/health/diet-goal)에 저장하고 건강 화면에서 정한다 (이슈 #131).
// 여기엔 서버에 자리가 없는 주간 공부 목표만 남긴다.
export interface Goals {
  studyWeekMinutes: number // 주간 공부 목표 (분)
}

export const DEFAULT_GOALS: Goals = {
  studyWeekMinutes: 600,
}

const stored = createStore<Goals>('hubdash.goals', DEFAULT_GOALS)
// 예전에 저장된 식단 목표 값은 버리고 필요한 항목만 남긴다
stored.set((g) => ({ studyWeekMinutes: g.studyWeekMinutes ?? DEFAULT_GOALS.studyWeekMinutes }))
export const goalsStore = stored
