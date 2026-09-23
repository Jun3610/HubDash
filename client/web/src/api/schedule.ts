import { defineResource } from './resource'
import type { EventRequest, ScheduleEvent } from './types'

export const events = defineResource<ScheduleEvent, EventRequest>('/api/schedule/events')
