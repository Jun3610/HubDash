package com.junyoung.dashboard.domain.life.service;

import com.junyoung.dashboard.domain.life.dto.RawHabitLogRequest;
import com.junyoung.dashboard.domain.life.dto.RawHabitLogResponse;
import com.junyoung.dashboard.domain.life.entity.RawHabitLog;
import com.junyoung.dashboard.domain.life.event.RawHabitLogCreatedEvent;
import com.junyoung.dashboard.domain.life.repository.RawHabitLogRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RawHabitLogService {

    private final RawHabitLogRepository rawHabitLogRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RawHabitLogService(RawHabitLogRepository rawHabitLogRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.rawHabitLogRepository = rawHabitLogRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public RawHabitLogResponse create(RawHabitLogRequest request) {
        RawHabitLog saved = rawHabitLogRepository.save(new RawHabitLog(
                request.habitIdRaw(),
                request.performedAtRaw(),
                request.completedRaw(),
                request.notes()
        ));
        kafkaTemplate.send(KafkaTopicConfig.LIFE_RAW_HABIT_LOG_TOPIC, new RawHabitLogCreatedEvent(saved.getId()));
        return RawHabitLogResponse.from(saved);
    }

    public RawHabitLogResponse findById(Long id) {
        return RawHabitLogResponse.from(getOrThrow(id));
    }

    private RawHabitLog getOrThrow(Long id) {
        return rawHabitLogRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(RawHabitLog.class, id));
    }
}
