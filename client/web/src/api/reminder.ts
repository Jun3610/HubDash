import { defineResource } from './resource'
import type { Reminder, ReminderRequest } from './types'

export const reminders = defineResource<Reminder, ReminderRequest>('/api/reminder/reminders')
