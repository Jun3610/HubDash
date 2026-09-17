package com.junyoung.dashboard.domain.reminder.consumer;

import com.junyoung.dashboard.domain.reminder.dto.RawReminderRequest;
import com.junyoung.dashboard.domain.reminder.entity.RawReminder;
import com.junyoung.dashboard.domain.reminder.entity.RawStatus;
import com.junyoung.dashboard.domain.reminder.event.RawReminderCreatedEvent;
import com.junyoung.dashboard.domain.reminder.repository.RawReminderRepository;
import com.junyoung.dashboard.domain.reminder.repository.ReminderRepository;
import com.junyoung.dashboard.domain.reminder.service.RawReminderService;
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
@EmbeddedKafka(partitions = 1, topics = "reminder-raw")
@ActiveProfiles("test")
class RawReminderConsumerIntegrationTest {

    @Autowired
    private RawReminderService rawReminderService;

    @Autowired
    private RawReminderRepository rawReminderRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private RawReminderConsumer rawReminderConsumer;

    @Test
    void validRawReminderIsProcessedAndNormalizedIntoReminder() {
        long reminderCountBefore = reminderRepository.count();

        var response = rawReminderService.create(
                new RawReminderRequest("정상 리마인더", "2026-09-20T10:00:00", "life", 1L, "false"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawReminder raw = rawReminderRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.PROCESSED);
        });

        assertThat(reminderRepository.count()).isEqualTo(reminderCountBefore + 1);
    }

    @Test
    void unparsableTargetAtMarksRawReminderAsFailedWithoutCreatingReminder() {
        // RawReminderRequest 자체가 @NotBlank라 API 경로로는 완전히 비어있는 값이 못 들어오지만,
        // "형식이 이상한 날짜 문자열"은 DTO 검증을 통과하므로 API 경로 그대로 재현 가능하다.
        long reminderCountBefore = reminderRepository.count();

        var response = rawReminderService.create(
                new RawReminderRequest("잘못된 날짜", "not-a-date", null, null, "false"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawReminder raw = rawReminderRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("targetAtRaw 파싱 실패");
        });

        assertThat(reminderRepository.count()).isEqualTo(reminderCountBefore);
    }

    @Test
    void invalidSentRawMarksRawReminderAsFailedWithoutCreatingReminder() {
        long reminderCountBefore = reminderRepository.count();

        var response = rawReminderService.create(
                new RawReminderRequest("잘못된 sent", "2026-09-20T10:00:00", null, null, "yes"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawReminder raw = rawReminderRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("sentRaw가 true/false가 아님");
        });

        assertThat(reminderRepository.count()).isEqualTo(reminderCountBefore);
    }

    @Test
    void redeliveredEventForAlreadyProcessedRawReminderDoesNotDuplicateReminder() {
        var response = rawReminderService.create(
                new RawReminderRequest("재전달 리마인더", "2026-09-20T10:00:00", null, null, "true"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(rawReminderRepository.findById(response.id()).orElseThrow().getStatus())
                        .isEqualTo(RawStatus.PROCESSED));

        long reminderCountAfterFirstProcessing = reminderRepository.count();

        // 이벤트 재전달 시나리오를 시뮬레이션 — 이미 PROCESSED인 레코드에 대해 컨슈머 로직을 한 번 더 직접 호출.
        rawReminderConsumer.consume(new RawReminderCreatedEvent(response.id()));

        assertThat(reminderRepository.count()).isEqualTo(reminderCountAfterFirstProcessing);
    }
}
