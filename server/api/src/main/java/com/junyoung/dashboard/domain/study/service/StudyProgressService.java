package com.junyoung.dashboard.domain.study.service;

import com.junyoung.dashboard.domain.study.dto.StudyProgressRequest;
import com.junyoung.dashboard.domain.study.dto.StudyProgressResponse;
import com.junyoung.dashboard.domain.study.entity.StudyProgress;
import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import com.junyoung.dashboard.domain.study.repository.StudyProgressRepository;
import com.junyoung.dashboard.domain.study.repository.StudyTopicRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudyProgressService {

    private final StudyProgressRepository studyProgressRepository;
    private final StudyTopicRepository studyTopicRepository;

    public StudyProgressService(StudyProgressRepository studyProgressRepository, StudyTopicRepository studyTopicRepository) {
        this.studyProgressRepository = studyProgressRepository;
        this.studyTopicRepository = studyTopicRepository;
    }

    @Transactional
    public StudyProgressResponse create(StudyProgressRequest request) {
        StudyTopic topic = getTopicOrThrow(request.topicId());
        StudyProgress saved = studyProgressRepository.save(
                new StudyProgress(topic, request.studiedAt(), request.minutes(), request.notes()));
        return StudyProgressResponse.from(saved);
    }

    public List<StudyProgressResponse> findByTopicId(Long topicId) {
        return studyProgressRepository.findByTopicId(topicId).stream()
                .map(StudyProgressResponse::from)
                .toList();
    }

    public StudyProgressResponse findById(Long id) {
        return StudyProgressResponse.from(getOrThrow(id));
    }

    @Transactional
    public StudyProgressResponse update(Long id, StudyProgressRequest request) {
        StudyProgress progress = getOrThrow(id);
        StudyTopic topic = getTopicOrThrow(request.topicId());
        progress.update(topic, request.studiedAt(), request.minutes(), request.notes());
        return StudyProgressResponse.from(progress);
    }

    @Transactional
    public void delete(Long id) {
        studyProgressRepository.delete(getOrThrow(id));
    }

    private StudyProgress getOrThrow(Long id) {
        return studyProgressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("study progress " + id + " not found"));
    }

    private StudyTopic getTopicOrThrow(Long topicId) {
        return studyTopicRepository.findById(topicId)
                .orElseThrow(() -> new EntityNotFoundException("study topic " + topicId + " not found"));
    }
}
