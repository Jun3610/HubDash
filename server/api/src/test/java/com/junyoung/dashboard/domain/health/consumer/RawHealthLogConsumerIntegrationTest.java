package com.junyoung.dashboard.domain.health.consumer;

import com.junyoung.dashboard.domain.health.dto.RawHealthLogRequest;
import com.junyoung.dashboard.domain.health.entity.RawHealthLog;
import com.junyoung.dashboard.domain.health.entity.RawStatus;
import com.junyoung.dashboard.domain.health.repository.HealthLogRepository;
import com.junyoung.dashboard.domain.health.repository.RawHealthLogRepository;
import com.junyoung.dashboard.domain.health.service.RawHealthLogService;
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
@EmbeddedKafka(partitions = 1, topics = "health-raw-log")
@ActiveProfiles("test")
class RawHealthLogConsumerIntegrationTest {

    @Autowired
    private RawHealthLogService rawHealthLogService;

    @Autowired
    private RawHealthLogRepository rawHealthLogRepository;

    @Autowired
    private HealthLogRepository healthLogRepository;

    @Test
    void validRawLogIsProcessedAndNormalizedIntoHealthLog() {
        long healthLogCountBefore = healthLogRepository.count();

        var response = rawHealthLogService.create(
                new RawHealthLogRequest("2026-09-17", "70.5", 7.5, "정상 케이스"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawHealthLog raw = rawHealthLogRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.PROCESSED);
        });

        assertThat(healthLogRepository.count()).isEqualTo(healthLogCountBefore + 1);
    }

    @Test
    void invalidDateMarksRawLogAsFailedWithoutCreatingHealthLog() {
        long healthLogCountBefore = healthLogRepository.count();

        var response = rawHealthLogService.create(
                new RawHealthLogRequest("이건-날짜-아님", null, null, "실패 케이스"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawHealthLog raw = rawHealthLogRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("recordedAtRaw 파싱 실패");
        });

        assertThat(healthLogRepository.count()).isEqualTo(healthLogCountBefore);
    }

    @Test
    void outOfRangeSleepHoursMarksRawLogAsFailed() {
        var response = rawHealthLogService.create(
                new RawHealthLogRequest("2026-09-17", null, 30.0, null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawHealthLog raw = rawHealthLogRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("sleepHoursRaw가 범위");
        });
    }
}
