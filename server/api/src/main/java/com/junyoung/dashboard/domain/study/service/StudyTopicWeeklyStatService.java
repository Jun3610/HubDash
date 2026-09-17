package com.junyoung.dashboard.domain.study.service;

import com.junyoung.dashboard.domain.study.dto.StudyTopicWeeklyStatResponse;
import com.junyoung.dashboard.domain.study.repository.StudyTopicWeeklyStatRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudyTopicWeeklyStatService {

    private final StudyTopicWeeklyStatRepository studyTopicWeeklyStatRepository;

    public StudyTopicWeeklyStatService(StudyTopicWeeklyStatRepository studyTopicWeeklyStatRepository) {
        this.studyTopicWeeklyStatRepository = studyTopicWeeklyStatRepository;
    }

    public Page<StudyTopicWeeklyStatResponse> findByTopicId(Long topicId, Pageable pageable) {
        return studyTopicWeeklyStatRepository.findByTopicId(topicId, pageable)
                .map(StudyTopicWeeklyStatResponse::from);
    }
}
