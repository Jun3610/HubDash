import { useMemo } from 'react'
import { hubCategories, hubLinks } from '../api/hub'
import { BIG_PAGE, useList, useListsByParent } from '../api/resource'
import type { HubLink } from '../api/types'

export function useHubCategories() {
  return useList(hubCategories, { size: BIG_PAGE, sort: 'name,asc' })
}

/** 카테고리별 링크를 병렬 조회해 합친다 */
export function useAllHubLinks() {
  const cats = useHubCategories()
  const categories = useMemo(() => cats.data?.content ?? [], [cats.data])
  const ids = useMemo(() => categories.map((c) => c.id), [categories])
  const links = useListsByParent<HubLink>(hubLinks, 'categoryId', ids, { size: BIG_PAGE, sort: 'createdAt,desc' })
  return {
    categories,
    links: links.data,
    linksByCategory: links.byParent,
    isLoading: cats.isLoading || links.isLoading,
    error: cats.error ?? links.error,
    refetch: () => {
      void cats.refetch()
      void links.refetch()
    },
  }
}

/** "https://github.com/x" → "github.com" */
export function hostOf(url: string): string {
  try {
    return new URL(url).host.replace(/^www\./, '')
  } catch {
    return url
  }
}
