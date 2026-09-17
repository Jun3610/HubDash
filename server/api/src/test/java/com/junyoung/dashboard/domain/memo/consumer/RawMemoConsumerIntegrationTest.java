package com.junyoung.dashboard.domain.memo.consumer;

import com.junyoung.dashboard.domain.memo.dto.RawMemoRequest;
import com.junyoung.dashboard.domain.memo.entity.RawMemo;
import com.junyoung.dashboard.domain.memo.entity.RawStatus;
import com.junyoung.dashboard.domain.memo.event.RawMemoCreatedEvent;
import com.junyoung.dashboard.domain.memo.repository.MemoRepository;
import com.junyoung.dashboard.domain.memo.repository.RawMemoRepository;
import com.junyoung.dashboard.domain.memo.service.RawMemoService;
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
@EmbeddedKafka(partitions = 1, topics = "memo-raw")
@ActiveProfiles("test")
class RawMemoConsumerIntegrationTest {

    @Autowired
    private RawMemoService rawMemoService;

    @Autowired
    private RawMemoRepository rawMemoRepository;

    @Autowired
    private MemoRepository memoRepository;

    @Autowired
    private RawMemoConsumer rawMemoConsumer;

    @Test
    void validRawMemoIsProcessedAndNormalizedIntoMemo() {
        long memoCountBefore = memoRepository.count();

        var response = rawMemoService.create(new RawMemoRequest("정상 메모", "본문입니다", "test,demo"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawMemo raw = rawMemoRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.PROCESSED);
        });

        assertThat(memoRepository.count()).isEqualTo(memoCountBefore + 1);
    }

    @Test
    void blankTitleMarksRawMemoAsFailedWithoutCreatingMemo() {
        // RawMemoRequest 자체가 @NotBlank라 API 경로로는 blank titleRaw가 못 들어온다 — Consumer의 방어 로직은
        // "producer가 항상 검증했다"고 신뢰하지 않는다는 설계이므로, Repository로 직접 저장해 그 경로를 우회하고
        // Consumer 로직 자체를 검증한다.
        long memoCountBefore = memoRepository.count();
        RawMemo raw = rawMemoRepository.save(new RawMemo("   ", "본문", null));

        rawMemoConsumer.consume(new RawMemoCreatedEvent(raw.getId()));

        RawMemo reloaded = rawMemoRepository.findById(raw.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RawStatus.FAILED);
        assertThat(reloaded.getFailureReason()).contains("titleRaw가 비어있음");
        assertThat(memoRepository.count()).isEqualTo(memoCountBefore);
    }

    @Test
    void redeliveredEventForAlreadyProcessedRawMemoDoesNotDuplicateMemo() {
        var response = rawMemoService.create(new RawMemoRequest("재전달 메모", "본문", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(rawMemoRepository.findById(response.id()).orElseThrow().getStatus())
                        .isEqualTo(RawStatus.PROCESSED));

        long memoCountAfterFirstProcessing = memoRepository.count();

        // 이벤트 재전달 시나리오를 시뮬레이션 — 이미 PROCESSED인 레코드에 대해 컨슈머 로직을 한 번 더 직접 호출.
        rawMemoConsumer.consume(new RawMemoCreatedEvent(response.id()));

        assertThat(memoRepository.count()).isEqualTo(memoCountAfterFirstProcessing);
    }
}
