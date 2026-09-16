package com.junyoung.dashboard.domain.study.service;

import com.junyoung.dashboard.domain.study.dto.StudyTopicRequest;
import com.junyoung.dashboard.domain.study.dto.StudyTopicResponse;
import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import com.junyoung.dashboard.domain.study.repository.StudyTopicRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudyTopicService {

    private final StudyTopicRepository studyTopicRepository;

    public StudyTopicService(StudyTopicRepository studyTopicRepository) {
        this.studyTopicRepository = studyTopicRepository;
    }

    @Transactional
    public StudyTopicResponse create(StudyTopicRequest request) {
        StudyTopic saved = studyTopicRepository.save(
                new StudyTopic(request.name(), request.description()));
        return StudyTopicResponse.from(saved);
    }

    public Page<StudyTopicResponse> findAll(Pageable pageable) {
        return studyTopicRepository.findAll(pageable)
                .map(StudyTopicResponse::from);
    }

    public StudyTopicResponse findById(Long id) {
        return StudyTopicResponse.from(getOrThrow(id));
    }

    @Transactional
    public StudyTopicResponse update(Long id, StudyTopicRequest request) {
        StudyTopic topic = getOrThrow(id);
        topic.update(request.name(), request.description());
        return StudyTopicResponse.from(topic);
    }

    @Transactional
    public void delete(Long id) {
        studyTopicRepository.delete(getOrThrow(id));
    }

    private StudyTopic getOrThrow(Long id) {
        return studyTopicRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(StudyTopic.class, id));
    }
}
