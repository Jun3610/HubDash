package com.junyoung.dashboard.domain.study.service;

import com.junyoung.dashboard.domain.study.dto.RawStudyProgressRequest;
import com.junyoung.dashboard.domain.study.dto.RawStudyProgressResponse;
import com.junyoung.dashboard.domain.study.entity.RawStudyProgress;
import com.junyoung.dashboard.domain.study.event.RawStudyProgressCreatedEvent;
import com.junyoung.dashboard.domain.study.repository.RawStudyProgressRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RawStudyProgressService {

    private final RawStudyProgressRepository rawStudyProgressRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RawStudyProgressService(RawStudyProgressRepository rawStudyProgressRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.rawStudyProgressRepository = rawStudyProgressRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public RawStudyProgressResponse create(RawStudyProgressRequest request) {
        RawStudyProgress saved = rawStudyProgressRepository.save(new RawStudyProgress(
                request.topicIdRaw(),
                request.studiedAtRaw(),
                request.minutesRaw(),
                request.notes()
        ));
        kafkaTemplate.send(KafkaTopicConfig.STUDY_RAW_PROGRESS_TOPIC, new RawStudyProgressCreatedEvent(saved.getId()));
        return RawStudyProgressResponse.from(saved);
    }

    public RawStudyProgressResponse findById(Long id) {
        return RawStudyProgressResponse.from(getOrThrow(id));
    }

    private RawStudyProgress getOrThrow(Long id) {
        return rawStudyProgressRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(RawStudyProgress.class, id));
    }
}
