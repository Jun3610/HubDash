package com.junyoung.dashboard.domain.reminder.consumer;

import com.junyoung.dashboard.domain.reminder.entity.RawReminder;
import com.junyoung.dashboard.domain.reminder.entity.RawStatus;
import com.junyoung.dashboard.domain.reminder.entity.Reminder;
import com.junyoung.dashboard.domain.reminder.event.RawReminderCreatedEvent;
import com.junyoung.dashboard.domain.reminder.repository.RawReminderRepository;
import com.junyoung.dashboard.domain.reminder.repository.ReminderRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

// 검증 실패는 "일시적 장애"가 아니라 "비즈니스 실패"다 — 예외를 던져 Kafka가 무한 재시도/재배달하게 만들지 않고,
// RawReminder.status를 FAILED로 명시적으로 기록한 뒤 정상 종료(ack)한다.
@Component
public class RawReminderConsumer {

    private static final int TITLE_MAX_LENGTH = 200;

    private static final Logger log = LoggerFactory.getLogger(RawReminderConsumer.class);

    private final RawReminderRepository rawReminderRepository;
    private final ReminderRepository reminderRepository;

    public RawReminderConsumer(RawReminderRepository rawReminderRepository, ReminderRepository reminderRepository) {
        this.rawReminderRepository = rawReminderRepository;
        this.reminderRepository = reminderRepository;
    }

    @KafkaListener(topics = KafkaTopicConfig.REMINDER_RAW_TOPIC)
    @Transactional
    public void consume(RawReminderCreatedEvent event) {
        RawReminder raw = rawReminderRepository.findById(event.rawReminderId()).orElse(null);
        if (raw == null) {
            log.warn("RawReminder {} not found, skipping", event.rawReminderId());
            return;
        }
        // Kafka는 at-least-once 전달을 보장한다 — 크래시/리밸런싱으로 같은 이벤트가 재전달될 수 있으므로,
        // 이미 처리 끝난(PROCESSED/FAILED) 레코드는 중복 처리(Reminder 중복 생성)하지 않도록 건너뛴다.
        if (raw.getStatus() != RawStatus.PENDING) {
            log.info("RawReminder {} already in status {}, skipping duplicate delivery", raw.getId(), raw.getStatus());
            return;
        }

        String titleRaw = raw.getTitleRaw();
        if (titleRaw == null || titleRaw.isBlank()) {
            raw.markFailed("titleRaw가 비어있음");
            return;
        }
        if (titleRaw.length() > TITLE_MAX_LENGTH) {
            raw.markFailed("titleRaw가 " + TITLE_MAX_LENGTH + "자를 초과함: " + titleRaw.length() + "자");
            return;
        }

        LocalDateTime targetAt;
        try {
            targetAt = LocalDateTime.parse(raw.getTargetAtRaw());
        } catch (DateTimeParseException e) {
            raw.markFailed("targetAtRaw 파싱 실패: " + raw.getTargetAtRaw());
            return;
        }

        Boolean sent = parseBoolean(raw.getSentRaw());
        if (sent == null) {
            raw.markFailed("sentRaw가 true/false가 아님: " + raw.getSentRaw());
            return;
        }

        reminderRepository.save(new Reminder(titleRaw, targetAt, raw.getTargetDomain(), raw.getTargetEntityId(), sent));
        raw.markProcessed();
    }

    private Boolean parseBoolean(String raw) {
        if ("true".equalsIgnoreCase(raw)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(raw)) {
            return Boolean.FALSE;
        }
        return null;
    }
}
