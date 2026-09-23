import { defineResource } from './resource'
import type { HubCategory, HubCategoryRequest, HubLink, HubLinkRequest } from './types'

export const hubCategories = defineResource<HubCategory, HubCategoryRequest>('/api/hub/categories')
/** 목록 필수 쿼리: categoryId */
export const hubLinks = defineResource<HubLink, HubLinkRequest>('/api/hub/links')
