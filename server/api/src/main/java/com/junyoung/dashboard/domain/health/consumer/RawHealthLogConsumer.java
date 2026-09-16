package com.junyoung.dashboard.domain.health.consumer;

import com.junyoung.dashboard.domain.health.entity.HealthLog;
import com.junyoung.dashboard.domain.health.entity.RawHealthLog;
import com.junyoung.dashboard.domain.health.event.RawHealthLogCreatedEvent;
import com.junyoung.dashboard.domain.health.repository.HealthLogRepository;
import com.junyoung.dashboard.domain.health.repository.RawHealthLogRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// 검증 실패는 "일시적 장애"가 아니라 "비즈니스 실패"다 — 예외를 던져 Kafka가 무한 재시도/재배달하게 만들지 않고,
// RawHealthLog.status를 FAILED로 명시적으로 기록한 뒤 정상 종료(ack)한다.
@Component
public class RawHealthLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(RawHealthLogConsumer.class);

    private final RawHealthLogRepository rawHealthLogRepository;
    private final HealthLogRepository healthLogRepository;

    public RawHealthLogConsumer(RawHealthLogRepository rawHealthLogRepository, HealthLogRepository healthLogRepository) {
        this.rawHealthLogRepository = rawHealthLogRepository;
        this.healthLogRepository = healthLogRepository;
    }

    @KafkaListener(topics = KafkaTopicConfig.HEALTH_RAW_LOG_TOPIC)
    @Transactional
    public void consume(RawHealthLogCreatedEvent event) {
        RawHealthLog raw = rawHealthLogRepository.findById(event.rawHealthLogId()).orElse(null);
        if (raw == null) {
            log.warn("RawHealthLog {} not found, skipping", event.rawHealthLogId());
            return;
        }

        LocalDate recordedAt;
        try {
            recordedAt = LocalDate.parse(raw.getRecordedAtRaw());
        } catch (DateTimeParseException e) {
            raw.markFailed("recordedAtRaw 파싱 실패: " + raw.getRecordedAtRaw());
            return;
        }

        Double weightKg = null;
        String weightKgRaw = raw.getWeightKgRaw();
        if (weightKgRaw != null && !weightKgRaw.isBlank()) {
            try {
                weightKg = Double.parseDouble(weightKgRaw);
            } catch (NumberFormatException e) {
                raw.markFailed("weightKgRaw 파싱 실패: " + weightKgRaw);
                return;
            }
            if (weightKg <= 0) {
                raw.markFailed("weightKgRaw가 양수가 아님: " + weightKgRaw);
                return;
            }
        }

        Double sleepHours = raw.getSleepHoursRaw();
        if (sleepHours != null && (sleepHours < 0.0 || sleepHours > 24.0)) {
            raw.markFailed("sleepHoursRaw가 범위(0~24) 밖: " + sleepHours);
            return;
        }

        healthLogRepository.save(new HealthLog(recordedAt, weightKg, sleepHours, raw.getNotes()));
        raw.markProcessed();
    }
}
