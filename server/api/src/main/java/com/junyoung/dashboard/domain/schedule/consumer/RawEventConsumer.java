package com.junyoung.dashboard.domain.schedule.consumer;

import com.junyoung.dashboard.domain.schedule.entity.Event;
import com.junyoung.dashboard.domain.schedule.entity.RawEvent;
import com.junyoung.dashboard.domain.schedule.entity.RawStatus;
import com.junyoung.dashboard.domain.schedule.event.RawEventCreatedEvent;
import com.junyoung.dashboard.domain.schedule.repository.EventRepository;
import com.junyoung.dashboard.domain.schedule.repository.RawEventRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

// 검증 실패는 "일시적 장애"가 아니라 "비즈니스 실패"다 — 예외를 던져 Kafka가 무한 재시도/재배달하게 만들지 않고,
// RawEvent.status를 FAILED로 명시적으로 기록한 뒤 정상 종료(ack)한다.
@Component
public class RawEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RawEventConsumer.class);

    private final RawEventRepository rawEventRepository;
    private final EventRepository eventRepository;

    public RawEventConsumer(RawEventRepository rawEventRepository, EventRepository eventRepository) {
        this.rawEventRepository = rawEventRepository;
        this.eventRepository = eventRepository;
    }

    @KafkaListener(topics = KafkaTopicConfig.SCHEDULE_RAW_EVENT_TOPIC)
    @Transactional
    public void consume(RawEventCreatedEvent event) {
        RawEvent raw = rawEventRepository.findById(event.rawEventId()).orElse(null);
        if (raw == null) {
            log.warn("RawEvent {} not found, skipping", event.rawEventId());
            return;
        }
        // Kafka는 at-least-once 전달을 보장한다 — 크래시/리밸런싱으로 같은 이벤트가 재전달될 수 있으므로,
        // 이미 처리 끝난(PROCESSED/FAILED) 레코드는 중복 처리(Event 중복 생성)하지 않도록 건너뛴다.
        if (raw.getStatus() != RawStatus.PENDING) {
            log.info("RawEvent {} already in status {}, skipping duplicate delivery", raw.getId(), raw.getStatus());
            return;
        }

        LocalDateTime startAt;
        try {
            startAt = LocalDateTime.parse(raw.getStartAtRaw());
        } catch (DateTimeParseException e) {
            raw.markFailed("startAtRaw 파싱 실패: " + raw.getStartAtRaw());
            return;
        }

        LocalDateTime endAt;
        try {
            endAt = LocalDateTime.parse(raw.getEndAtRaw());
        } catch (DateTimeParseException e) {
            raw.markFailed("endAtRaw 파싱 실패: " + raw.getEndAtRaw());
            return;
        }

        if (!endAt.isAfter(startAt)) {
            raw.markFailed("endAt가 startAt보다 이후가 아님: startAt=" + startAt + ", endAt=" + endAt);
            return;
        }

        Boolean allDay;
        String allDayRaw = raw.getAllDayRaw();
        if ("true".equalsIgnoreCase(allDayRaw)) {
            allDay = true;
        } else if ("false".equalsIgnoreCase(allDayRaw)) {
            allDay = false;
        } else {
            raw.markFailed("allDayRaw가 올바른 boolean 값이 아님: " + allDayRaw);
            return;
        }

        eventRepository.save(new Event(raw.getTitleRaw(), startAt, endAt, raw.getLocation(), raw.getDescription(), allDay));
        raw.markProcessed();
    }
}
