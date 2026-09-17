package com.junyoung.dashboard.domain.schedule.service;

import com.junyoung.dashboard.domain.schedule.dto.RawEventRequest;
import com.junyoung.dashboard.domain.schedule.dto.RawEventResponse;
import com.junyoung.dashboard.domain.schedule.entity.RawEvent;
import com.junyoung.dashboard.domain.schedule.event.RawEventCreatedEvent;
import com.junyoung.dashboard.domain.schedule.repository.RawEventRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RawEventService {

    private final RawEventRepository rawEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RawEventService(RawEventRepository rawEventRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.rawEventRepository = rawEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public RawEventResponse create(RawEventRequest request) {
        RawEvent saved = rawEventRepository.save(new RawEvent(
                request.titleRaw(), request.startAtRaw(), request.endAtRaw(),
                request.location(), request.description(), request.allDayRaw()));
        kafkaTemplate.send(KafkaTopicConfig.SCHEDULE_RAW_EVENT_TOPIC, new RawEventCreatedEvent(saved.getId()));
        return RawEventResponse.from(saved);
    }

    public RawEventResponse findById(Long id) {
        return RawEventResponse.from(getOrThrow(id));
    }

    private RawEvent getOrThrow(Long id) {
        return rawEventRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(RawEvent.class, id));
    }
}
