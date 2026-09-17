package com.junyoung.dashboard.domain.study.consumer;

import com.junyoung.dashboard.domain.study.entity.RawStatus;
import com.junyoung.dashboard.domain.study.entity.RawStudyProgress;
import com.junyoung.dashboard.domain.study.entity.StudyProgress;
import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import com.junyoung.dashboard.domain.study.event.RawStudyProgressCreatedEvent;
import com.junyoung.dashboard.domain.study.repository.RawStudyProgressRepository;
import com.junyoung.dashboard.domain.study.repository.StudyProgressRepository;
import com.junyoung.dashboard.domain.study.repository.StudyTopicRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// 검증 실패는 "일시적 장애"가 아니라 "비즈니스 실패"다 — 예외를 던져 Kafka가 무한 재시도/재배달하게 만들지 않고,
// RawStudyProgress.status를 FAILED로 명시적으로 기록한 뒤 정상 종료(ack)한다.
// 부모(StudyTopic) 존재 확인도 같은 원칙 — EntityNotFoundException을 던지지 않고 findById 결과를 직접 체크한다.
@Component
public class RawStudyProgressConsumer {

    private static final int MAX_MINUTES = 1440;

    private static final Logger log = LoggerFactory.getLogger(RawStudyProgressConsumer.class);

    private final RawStudyProgressRepository rawStudyProgressRepository;
    private final StudyProgressRepository studyProgressRepository;
    private final StudyTopicRepository studyTopicRepository;

    public RawStudyProgressConsumer(RawStudyProgressRepository rawStudyProgressRepository,
                                     StudyProgressRepository studyProgressRepository,
                                     StudyTopicRepository studyTopicRepository) {
        this.rawStudyProgressRepository = rawStudyProgressRepository;
        this.studyProgressRepository = studyProgressRepository;
        this.studyTopicRepository = studyTopicRepository;
    }

    @KafkaListener(topics = KafkaTopicConfig.STUDY_RAW_PROGRESS_TOPIC)
    @Transactional
    public void consume(RawStudyProgressCreatedEvent event) {
        RawStudyProgress raw = rawStudyProgressRepository.findById(event.rawStudyProgressId()).orElse(null);
        if (raw == null) {
            log.warn("RawStudyProgress {} not found, skipping", event.rawStudyProgressId());
            return;
        }
        // Kafka는 at-least-once 전달을 보장한다 — 크래시/리밸런싱으로 같은 이벤트가 재전달될 수 있으므로,
        // 이미 처리 끝난(PROCESSED/FAILED) 레코드는 중복 처리(StudyProgress 중복 생성)하지 않도록 건너뛴다.
        if (raw.getStatus() != RawStatus.PENDING) {
            log.info("RawStudyProgress {} already in status {}, skipping duplicate delivery", raw.getId(), raw.getStatus());
            return;
        }

        Long topicId;
        try {
            topicId = Long.parseLong(raw.getTopicIdRaw());
        } catch (NumberFormatException e) {
            raw.markFailed("topicIdRaw 파싱 실패: " + raw.getTopicIdRaw());
            return;
        }

        StudyTopic topic = studyTopicRepository.findById(topicId).orElse(null);
        if (topic == null) {
            raw.markFailed("topicId " + topicId + " not found");
            return;
        }

        LocalDate studiedAt;
        try {
            studiedAt = LocalDate.parse(raw.getStudiedAtRaw());
        } catch (DateTimeParseException e) {
            raw.markFailed("studiedAtRaw 파싱 실패: " + raw.getStudiedAtRaw());
            return;
        }

        Integer minutes;
        try {
            minutes = Integer.parseInt(raw.getMinutesRaw());
        } catch (NumberFormatException e) {
            raw.markFailed("minutesRaw 파싱 실패: " + raw.getMinutesRaw());
            return;
        }
        if (minutes <= 0) {
            raw.markFailed("minutesRaw가 양수가 아님: " + minutes);
            return;
        }
        if (minutes > MAX_MINUTES) {
            raw.markFailed("minutesRaw가 " + MAX_MINUTES + "을 초과함: " + minutes);
            return;
        }

        StudyProgress saved = new StudyProgress(topic, studiedAt, minutes, raw.getNotes());
        studyProgressRepository.save(saved);
        raw.markProcessed();
    }
}
