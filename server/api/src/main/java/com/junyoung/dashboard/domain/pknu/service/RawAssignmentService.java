package com.junyoung.dashboard.domain.pknu.service;

import com.junyoung.dashboard.domain.pknu.dto.RawAssignmentRequest;
import com.junyoung.dashboard.domain.pknu.dto.RawAssignmentResponse;
import com.junyoung.dashboard.domain.pknu.entity.RawAssignment;
import com.junyoung.dashboard.domain.pknu.event.RawAssignmentCreatedEvent;
import com.junyoung.dashboard.domain.pknu.repository.RawAssignmentRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RawAssignmentService {

    private final RawAssignmentRepository rawAssignmentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RawAssignmentService(RawAssignmentRepository rawAssignmentRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.rawAssignmentRepository = rawAssignmentRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public RawAssignmentResponse create(RawAssignmentRequest request) {
        RawAssignment saved = rawAssignmentRepository.save(new RawAssignment(
                request.courseIdRaw(),
                request.titleRaw(),
                request.dueDateRaw(),
                request.completedRaw(),
                request.notes()
        ));
        kafkaTemplate.send(KafkaTopicConfig.PKNU_RAW_ASSIGNMENT_TOPIC, new RawAssignmentCreatedEvent(saved.getId()));
        return RawAssignmentResponse.from(saved);
    }

    public RawAssignmentResponse findById(Long id) {
        return RawAssignmentResponse.from(getOrThrow(id));
    }

    private RawAssignment getOrThrow(Long id) {
        return rawAssignmentRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(RawAssignment.class, id));
    }
}
