package com.junyoung.dashboard.domain.schedule.consumer;

import com.junyoung.dashboard.domain.schedule.dto.RawEventRequest;
import com.junyoung.dashboard.domain.schedule.entity.RawEvent;
import com.junyoung.dashboard.domain.schedule.entity.RawStatus;
import com.junyoung.dashboard.domain.schedule.event.RawEventCreatedEvent;
import com.junyoung.dashboard.domain.schedule.repository.EventRepository;
import com.junyoung.dashboard.domain.schedule.repository.RawEventRepository;
import com.junyoung.dashboard.domain.schedule.service.RawEventService;
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
@EmbeddedKafka(partitions = 1, topics = "schedule-raw-event")
@ActiveProfiles("test")
class RawEventConsumerIntegrationTest {

    @Autowired
    private RawEventService rawEventService;

    @Autowired
    private RawEventRepository rawEventRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RawEventConsumer rawEventConsumer;

    @Test
    void validRawEventIsProcessedAndNormalizedIntoEvent() {
        long eventCountBefore = eventRepository.count();

        var response = rawEventService.create(new RawEventRequest(
                "정상 이벤트", "2026-09-17T10:00:00", "2026-09-17T11:00:00", "회의실", "설명", "false"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawEvent raw = rawEventRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.PROCESSED);
        });

        assertThat(eventRepository.count()).isEqualTo(eventCountBefore + 1);
    }

    @Test
    void invalidStartAtMarksRawEventAsFailedWithoutCreatingEvent() {
        long eventCountBefore = eventRepository.count();

        var response = rawEventService.create(new RawEventRequest(
                "실패 이벤트", "이건-날짜-아님", "2026-09-17T11:00:00", null, null, "false"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawEvent raw = rawEventRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("startAtRaw 파싱 실패");
        });

        assertThat(eventRepository.count()).isEqualTo(eventCountBefore);
    }

    @Test
    void endAtBeforeStartAtMarksRawEventAsFailed() {
        var response = rawEventService.create(new RawEventRequest(
                "순서 오류 이벤트", "2026-09-17T12:00:00", "2026-09-17T11:00:00", null, null, "false"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawEvent raw = rawEventRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("endAt가 startAt보다 이후가 아님");
        });
    }

    @Test
    void redeliveredEventForAlreadyProcessedRawEventDoesNotDuplicateEvent() {
        var response = rawEventService.create(new RawEventRequest(
                "재전달 이벤트", "2026-09-17T09:00:00", "2026-09-17T10:00:00", null, null, "true"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(rawEventRepository.findById(response.id()).orElseThrow().getStatus())
                        .isEqualTo(RawStatus.PROCESSED));

        long eventCountAfterFirstProcessing = eventRepository.count();

        // 이벤트 재전달 시나리오를 시뮬레이션 — 이미 PROCESSED인 레코드에 대해 컨슈머 로직을 한 번 더 직접 호출.
        rawEventConsumer.consume(new RawEventCreatedEvent(response.id()));

        assertThat(eventRepository.count()).isEqualTo(eventCountAfterFirstProcessing);
    }
}
