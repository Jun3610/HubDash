package com.junyoung.dashboard.domain.life.consumer;

import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.entity.HabitLog;
import com.junyoung.dashboard.domain.life.entity.RawHabitLog;
import com.junyoung.dashboard.domain.life.entity.RawStatus;
import com.junyoung.dashboard.domain.life.event.RawHabitLogCreatedEvent;
import com.junyoung.dashboard.domain.life.repository.HabitLogRepository;
import com.junyoung.dashboard.domain.life.repository.HabitRepository;
import com.junyoung.dashboard.domain.life.repository.RawHabitLogRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// 검증 실패는 "일시적 장애"가 아니라 "비즈니스 실패"다 — 예외를 던져 Kafka가 무한 재시도/재배달하게 만들지 않고,
// RawHabitLog.status를 FAILED로 명시적으로 기록한 뒤 정상 종료(ack)한다.
// 부모(Habit) 존재 확인도 같은 원칙 — EntityNotFoundException을 던지지 않고 findById 결과를 직접 체크한다.
@Component
public class RawHabitLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(RawHabitLogConsumer.class);

    private final RawHabitLogRepository rawHabitLogRepository;
    private final HabitLogRepository habitLogRepository;
    private final HabitRepository habitRepository;

    public RawHabitLogConsumer(RawHabitLogRepository rawHabitLogRepository,
                                HabitLogRepository habitLogRepository,
                                HabitRepository habitRepository) {
        this.rawHabitLogRepository = rawHabitLogRepository;
        this.habitLogRepository = habitLogRepository;
        this.habitRepository = habitRepository;
    }

    @KafkaListener(topics = KafkaTopicConfig.LIFE_RAW_HABIT_LOG_TOPIC)
    @Transactional
    public void consume(RawHabitLogCreatedEvent event) {
        RawHabitLog raw = rawHabitLogRepository.findById(event.rawHabitLogId()).orElse(null);
        if (raw == null) {
            log.warn("RawHabitLog {} not found, skipping", event.rawHabitLogId());
            return;
        }
        // Kafka는 at-least-once 전달을 보장한다 — 크래시/리밸런싱으로 같은 이벤트가 재전달될 수 있으므로,
        // 이미 처리 끝난(PROCESSED/FAILED) 레코드는 중복 처리(HabitLog 중복 생성)하지 않도록 건너뛴다.
        if (raw.getStatus() != RawStatus.PENDING) {
            log.info("RawHabitLog {} already in status {}, skipping duplicate delivery", raw.getId(), raw.getStatus());
            return;
        }

        Long habitId;
        try {
            habitId = Long.parseLong(raw.getHabitIdRaw());
        } catch (NumberFormatException e) {
            raw.markFailed("habitIdRaw 파싱 실패: " + raw.getHabitIdRaw());
            return;
        }

        Habit habit = habitRepository.findById(habitId).orElse(null);
        if (habit == null) {
            raw.markFailed("habitId " + habitId + " not found");
            return;
        }

        LocalDate performedAt;
        try {
            performedAt = LocalDate.parse(raw.getPerformedAtRaw());
        } catch (DateTimeParseException e) {
            raw.markFailed("performedAtRaw 파싱 실패: " + raw.getPerformedAtRaw());
            return;
        }

        // reminder 확장 교훈: Boolean.parseBoolean()은 "yes"/"1" 같은 모호한 값을 조용히 false로 취급한다 —
        // "true"/"false" 문자열만 엄격하게 허용하고, 그 외는 명시적으로 FAILED 처리한다.
        Boolean completed;
        if ("true".equals(raw.getCompletedRaw())) {
            completed = Boolean.TRUE;
        } else if ("false".equals(raw.getCompletedRaw())) {
            completed = Boolean.FALSE;
        } else {
            raw.markFailed("completedRaw가 true/false가 아님: " + raw.getCompletedRaw());
            return;
        }

        habitLogRepository.save(new HabitLog(habit, performedAt, completed, raw.getNotes()));
        raw.markProcessed();
    }
}
