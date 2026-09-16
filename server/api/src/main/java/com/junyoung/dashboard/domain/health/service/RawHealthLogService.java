package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.RawHealthLogRequest;
import com.junyoung.dashboard.domain.health.dto.RawHealthLogResponse;
import com.junyoung.dashboard.domain.health.entity.RawHealthLog;
import com.junyoung.dashboard.domain.health.event.RawHealthLogCreatedEvent;
import com.junyoung.dashboard.domain.health.repository.RawHealthLogRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RawHealthLogService {

    private final RawHealthLogRepository rawHealthLogRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RawHealthLogService(RawHealthLogRepository rawHealthLogRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.rawHealthLogRepository = rawHealthLogRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public RawHealthLogResponse create(RawHealthLogRequest request) {
        RawHealthLog saved = rawHealthLogRepository.save(new RawHealthLog(
                request.recordedAtRaw(), request.weightKgRaw(), request.sleepHoursRaw(), request.notes()));
        kafkaTemplate.send(KafkaTopicConfig.HEALTH_RAW_LOG_TOPIC, new RawHealthLogCreatedEvent(saved.getId()));
        return RawHealthLogResponse.from(saved);
    }

    public RawHealthLogResponse findById(Long id) {
        return RawHealthLogResponse.from(getOrThrow(id));
    }

    private RawHealthLog getOrThrow(Long id) {
        return rawHealthLogRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(RawHealthLog.class, id));
    }
}
