import { defineResource } from './resource'
import type { StudyProgress, StudyProgressRequest, StudyTopic, StudyTopicRequest } from './types'

export const studyTopics = defineResource<StudyTopic, StudyTopicRequest>('/api/study/topics')
/** 목록 필수 쿼리: topicId */
export const studyProgresses = defineResource<StudyProgress, StudyProgressRequest>('/api/study/progresses')
