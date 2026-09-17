package com.junyoung.dashboard.domain.hub.consumer;

import com.junyoung.dashboard.domain.hub.dto.RawHubLinkRequest;
import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.entity.RawHubLink;
import com.junyoung.dashboard.domain.hub.entity.RawStatus;
import com.junyoung.dashboard.domain.hub.event.RawHubLinkCreatedEvent;
import com.junyoung.dashboard.domain.hub.repository.HubCategoryRepository;
import com.junyoung.dashboard.domain.hub.repository.HubLinkRepository;
import com.junyoung.dashboard.domain.hub.repository.RawHubLinkRepository;
import com.junyoung.dashboard.domain.hub.service.RawHubLinkService;
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
@EmbeddedKafka(partitions = 1, topics = "hub-raw-link")
@ActiveProfiles("test")
class RawHubLinkConsumerIntegrationTest {

    @Autowired
    private RawHubLinkService rawHubLinkService;

    @Autowired
    private RawHubLinkRepository rawHubLinkRepository;

    @Autowired
    private HubLinkRepository hubLinkRepository;

    @Autowired
    private HubCategoryRepository hubCategoryRepository;

    @Autowired
    private RawHubLinkConsumer rawHubLinkConsumer;

    @Test
    void validRawHubLinkIsProcessedAndNormalizedIntoHubLink() {
        HubCategory category = hubCategoryRepository.save(new HubCategory("개발", "개발 관련 링크"));
        long hubLinkCountBefore = hubLinkRepository.count();

        var response = rawHubLinkService.create(
                new RawHubLinkRequest(category.getId().toString(), "GitHub", "https://github.com", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawHubLink raw = rawHubLinkRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.PROCESSED);
        });

        assertThat(hubLinkRepository.count()).isEqualTo(hubLinkCountBefore + 1);
    }

    @Test
    void missingCategoryMarksRawHubLinkAsFailedWithoutCreatingHubLink() {
        long hubLinkCountBefore = hubLinkRepository.count();

        var response = rawHubLinkService.create(
                new RawHubLinkRequest("999999", "존재하지 않는 카테고리", "https://example.com", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawHubLink raw = rawHubLinkRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("categoryId 999999 not found");
        });

        assertThat(hubLinkRepository.count()).isEqualTo(hubLinkCountBefore);
    }

    @Test
    void unparsableCategoryIdMarksRawHubLinkAsFailedWithoutCreatingHubLink() {
        long hubLinkCountBefore = hubLinkRepository.count();

        var response = rawHubLinkService.create(
                new RawHubLinkRequest("not-a-number", "잘못된 카테고리 id", "https://example.com", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawHubLink raw = rawHubLinkRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("categoryIdRaw 파싱 실패");
        });

        assertThat(hubLinkRepository.count()).isEqualTo(hubLinkCountBefore);
    }

    @Test
    void redeliveredEventForAlreadyProcessedRawHubLinkDoesNotDuplicateHubLink() {
        HubCategory category = hubCategoryRepository.save(new HubCategory("재전달 테스트", null));

        var response = rawHubLinkService.create(
                new RawHubLinkRequest(category.getId().toString(), "재전달 링크", "https://example.com/redelivery", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(rawHubLinkRepository.findById(response.id()).orElseThrow().getStatus())
                        .isEqualTo(RawStatus.PROCESSED));

        long hubLinkCountAfterFirstProcessing = hubLinkRepository.count();

        // 이벤트 재전달 시나리오를 시뮬레이션 — 이미 PROCESSED인 레코드에 대해 컨슈머 로직을 한 번 더 직접 호출.
        rawHubLinkConsumer.consume(new RawHubLinkCreatedEvent(response.id()));

        assertThat(hubLinkRepository.count()).isEqualTo(hubLinkCountAfterFirstProcessing);
    }
}
