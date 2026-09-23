import { defineResource } from './resource'
import type { Memo, MemoRequest } from './types'

export const memos = defineResource<Memo, MemoRequest>('/api/memo/memos')
