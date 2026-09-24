import { useList } from '../api/resource'
import { events } from '../api/schedule'
import { YEAR_PAGE } from './useActivity'

/** 일정 전체. 날짜 범위 필터가 없어 넉넉히 받아 프론트에서 거른다 (홈·일정이 캐시 공유) */
export function useAllEvents() {
  return useList(events, { size: YEAR_PAGE, sort: 'startAt,desc' })
}
