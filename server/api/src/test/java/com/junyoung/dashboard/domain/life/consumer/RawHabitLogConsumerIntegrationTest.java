package com.junyoung.dashboard.domain.life.consumer;

import com.junyoung.dashboard.domain.life.dto.RawHabitLogRequest;
import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.entity.RawHabitLog;
import com.junyoung.dashboard.domain.life.entity.RawStatus;
import com.junyoung.dashboard.domain.life.event.RawHabitLogCreatedEvent;
import com.junyoung.dashboard.domain.life.repository.HabitLogRepository;
import com.junyoung.dashboard.domain.life.repository.HabitRepository;
import com.junyoung.dashboard.domain.life.repository.RawHabitLogRepository;
import com.junyoung.dashboard.domain.life.service.RawHabitLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// 실제 Kafka publish -> Consumer consume 흐름을 EmbeddedKafka로 검증한다.
// 리스너 자동 시작(test 프로파일 기본값 false)과 부트스트랩 서버(기본값 localhost:9092)를 이 임베디드 브로커 기준으로 오버라이드한다.
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=true",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(partitions = 1, topics = "life-raw-habit-log")
@ActiveProfiles("test")
class RawHabitLogConsumerIntegrationTest {

    @Autowired
    private RawHabitLogService rawHabitLogService;

    @Autowired
    private RawHabitLogRepository rawHabitLogRepository;

    @Autowired
    private HabitLogRepository habitLogRepository;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private RawHabitLogConsumer rawHabitLogConsumer;

    @Test
    void validRawHabitLogIsProcessedAndNormalizedIntoHabitLog() {
        Habit habit = habitRepository.save(new Habit("아침 운동", "매일 아침 스트레칭"));
        long habitLogCountBefore = habitLogRepository.count();

        var response = rawHabitLogService.create(
                new RawHabitLogRequest(habit.getId().toString(), "2026-09-17", "true", "완료함"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawHabitLog raw = rawHabitLogRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.PROCESSED);
        });

        assertThat(habitLogRepository.count()).isEqualTo(habitLogCountBefore + 1);
    }

    @Test
    void missingHabitMarksRawHabitLogAsFailedWithoutCreatingHabitLog() {
        long habitLogCountBefore = habitLogRepository.count();

        var response = rawHabitLogService.create(
                new RawHabitLogRequest("999999", "2026-09-17", "true", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawHabitLog raw = rawHabitLogRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("habitId 999999 not found");
        });

        assertThat(habitLogRepository.count()).isEqualTo(habitLogCountBefore);
    }

    @Test
    void unparsablePerformedAtMarksRawHabitLogAsFailedWithoutCreatingHabitLog() {
        Habit habit = habitRepository.save(new Habit("날짜파싱 테스트", null));
        long habitLogCountBefore = habitLogRepository.count();

        var response = rawHabitLogService.create(
                new RawHabitLogRequest(habit.getId().toString(), "not-a-date", "true", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawHabitLog raw = rawHabitLogRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("performedAtRaw 파싱 실패");
        });

        assertThat(habitLogRepository.count()).isEqualTo(habitLogCountBefore);
    }

    @Test
    void ambiguousCompletedRawMarksRawHabitLogAsFailedWithoutCreatingHabitLog() {
        Habit habit = habitRepository.save(new Habit("모호값 테스트", null));
        long habitLogCountBefore = habitLogRepository.count();

        var response = rawHabitLogService.create(
                new RawHabitLogRequest(habit.getId().toString(), "2026-09-17", "yes", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawHabitLog raw = rawHabitLogRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("completedRaw가 true/false가 아님");
        });

        assertThat(habitLogRepository.count()).isEqualTo(habitLogCountBefore);
    }

    @Test
    void redeliveredEventForAlreadyProcessedRawHabitLogDoesNotDuplicateHabitLog() {
        Habit habit = habitRepository.save(new Habit("재전달 테스트", null));

        var response = rawHabitLogService.create(
                new RawHabitLogRequest(habit.getId().toString(), "2026-09-17", "false", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(rawHabitLogRepository.findById(response.id()).orElseThrow().getStatus())
                        .isEqualTo(RawStatus.PROCESSED));

        long habitLogCountAfterFirstProcessing = habitLogRepository.count();

        // 이벤트 재전달 시나리오를 시뮬레이션 — 이미 PROCESSED인 레코드에 대해 컨슈머 로직을 한 번 더 직접 호출.
        rawHabitLogConsumer.consume(new RawHabitLogCreatedEvent(response.id()));

        assertThat(habitLogRepository.count()).isEqualTo(habitLogCountAfterFirstProcessing);
    }
}
