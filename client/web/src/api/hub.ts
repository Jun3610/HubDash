import { request } from './client'
import { defineResource } from './resource'
import type { HubCategory, HubCategoryRequest, HubLink, HubLinkRequest } from './types'

export const hubCategories = defineResource<HubCategory, HubCategoryRequest>('/api/hub/categories')
/** 목록 필수 쿼리: categoryId */
export const hubLinks = defineResource<HubLink, HubLinkRequest>('/api/hub/links')

export interface NotionSyncResult {
  added: number
  categories: { categoryId: number; name: string; added: number; total: number }[]
}

/** 'Notion 불러오기': 노션 DB가 연결된 카테고리에서 아직 없는 글만 링크로 추가 (이슈 #163) */
export const syncNotion = () => request<NotionSyncResult>('/api/hub/notion-sync', { method: 'POST' })
