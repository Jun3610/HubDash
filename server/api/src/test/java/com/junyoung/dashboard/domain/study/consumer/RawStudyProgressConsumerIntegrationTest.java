package com.junyoung.dashboard.domain.study.consumer;

import com.junyoung.dashboard.domain.study.dto.RawStudyProgressRequest;
import com.junyoung.dashboard.domain.study.entity.RawStatus;
import com.junyoung.dashboard.domain.study.entity.RawStudyProgress;
import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import com.junyoung.dashboard.domain.study.event.RawStudyProgressCreatedEvent;
import com.junyoung.dashboard.domain.study.repository.RawStudyProgressRepository;
import com.junyoung.dashboard.domain.study.repository.StudyProgressRepository;
import com.junyoung.dashboard.domain.study.repository.StudyTopicRepository;
import com.junyoung.dashboard.domain.study.service.RawStudyProgressService;
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
@EmbeddedKafka(partitions = 1, topics = "study-raw-progress")
@ActiveProfiles("test")
class RawStudyProgressConsumerIntegrationTest {

    @Autowired
    private RawStudyProgressService rawStudyProgressService;

    @Autowired
    private RawStudyProgressRepository rawStudyProgressRepository;

    @Autowired
    private StudyProgressRepository studyProgressRepository;

    @Autowired
    private StudyTopicRepository studyTopicRepository;

    @Autowired
    private RawStudyProgressConsumer rawStudyProgressConsumer;

    @Test
    void validRawStudyProgressIsProcessedAndNormalizedIntoStudyProgress() {
        StudyTopic topic = studyTopicRepository.save(new StudyTopic("스프링", "스프링 공부"));
        long studyProgressCountBefore = studyProgressRepository.count();

        var response = rawStudyProgressService.create(
                new RawStudyProgressRequest(topic.getId().toString(), "2026-09-17", "90", "핵심 정리"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawStudyProgress raw = rawStudyProgressRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.PROCESSED);
        });

        assertThat(studyProgressRepository.count()).isEqualTo(studyProgressCountBefore + 1);
    }

    @Test
    void missingTopicMarksRawStudyProgressAsFailedWithoutCreatingStudyProgress() {
        long studyProgressCountBefore = studyProgressRepository.count();

        var response = rawStudyProgressService.create(
                new RawStudyProgressRequest("999999", "2026-09-17", "60", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawStudyProgress raw = rawStudyProgressRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("topicId 999999 not found");
        });

        assertThat(studyProgressRepository.count()).isEqualTo(studyProgressCountBefore);
    }

    @Test
    void outOfRangeMinutesMarksRawStudyProgressAsFailedWithoutCreatingStudyProgress() {
        StudyTopic topic = studyTopicRepository.save(new StudyTopic("범위초과 테스트", null));
        long studyProgressCountBefore = studyProgressRepository.count();

        var response = rawStudyProgressService.create(
                new RawStudyProgressRequest(topic.getId().toString(), "2026-09-17", "2000", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawStudyProgress raw = rawStudyProgressRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("minutesRaw가 1440을 초과함");
        });

        assertThat(studyProgressRepository.count()).isEqualTo(studyProgressCountBefore);
    }

    @Test
    void unparsableStudiedAtMarksRawStudyProgressAsFailedWithoutCreatingStudyProgress() {
        StudyTopic topic = studyTopicRepository.save(new StudyTopic("날짜파싱 테스트", null));
        long studyProgressCountBefore = studyProgressRepository.count();

        var response = rawStudyProgressService.create(
                new RawStudyProgressRequest(topic.getId().toString(), "not-a-date", "30", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawStudyProgress raw = rawStudyProgressRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("studiedAtRaw 파싱 실패");
        });

        assertThat(studyProgressRepository.count()).isEqualTo(studyProgressCountBefore);
    }

    @Test
    void redeliveredEventForAlreadyProcessedRawStudyProgressDoesNotDuplicateStudyProgress() {
        StudyTopic topic = studyTopicRepository.save(new StudyTopic("재전달 테스트", null));

        var response = rawStudyProgressService.create(
                new RawStudyProgressRequest(topic.getId().toString(), "2026-09-17", "45", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(rawStudyProgressRepository.findById(response.id()).orElseThrow().getStatus())
                        .isEqualTo(RawStatus.PROCESSED));

        long studyProgressCountAfterFirstProcessing = studyProgressRepository.count();

        // 이벤트 재전달 시나리오를 시뮬레이션 — 이미 PROCESSED인 레코드에 대해 컨슈머 로직을 한 번 더 직접 호출.
        rawStudyProgressConsumer.consume(new RawStudyProgressCreatedEvent(response.id()));

        assertThat(studyProgressRepository.count()).isEqualTo(studyProgressCountAfterFirstProcessing);
    }
}
