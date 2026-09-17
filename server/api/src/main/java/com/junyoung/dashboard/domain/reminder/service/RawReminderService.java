package com.junyoung.dashboard.domain.reminder.service;

import com.junyoung.dashboard.domain.reminder.dto.RawReminderRequest;
import com.junyoung.dashboard.domain.reminder.dto.RawReminderResponse;
import com.junyoung.dashboard.domain.reminder.entity.RawReminder;
import com.junyoung.dashboard.domain.reminder.event.RawReminderCreatedEvent;
import com.junyoung.dashboard.domain.reminder.repository.RawReminderRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RawReminderService {

    private final RawReminderRepository rawReminderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RawReminderService(RawReminderRepository rawReminderRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.rawReminderRepository = rawReminderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public RawReminderResponse create(RawReminderRequest request) {
        RawReminder saved = rawReminderRepository.save(new RawReminder(
                request.titleRaw(),
                request.targetAtRaw(),
                request.targetDomain(),
                request.targetEntityId(),
                request.sentRaw()
        ));
        kafkaTemplate.send(KafkaTopicConfig.REMINDER_RAW_TOPIC, new RawReminderCreatedEvent(saved.getId()));
        return RawReminderResponse.from(saved);
    }

    public RawReminderResponse findById(Long id) {
        return RawReminderResponse.from(getOrThrow(id));
    }

    private RawReminder getOrThrow(Long id) {
        return rawReminderRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(RawReminder.class, id));
    }
}
