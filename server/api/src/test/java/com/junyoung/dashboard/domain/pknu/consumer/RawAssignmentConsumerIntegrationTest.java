package com.junyoung.dashboard.domain.pknu.consumer;

import com.junyoung.dashboard.domain.pknu.dto.RawAssignmentRequest;
import com.junyoung.dashboard.domain.pknu.entity.Course;
import com.junyoung.dashboard.domain.pknu.entity.RawAssignment;
import com.junyoung.dashboard.domain.pknu.entity.RawStatus;
import com.junyoung.dashboard.domain.pknu.entity.Semester;
import com.junyoung.dashboard.domain.pknu.event.RawAssignmentCreatedEvent;
import com.junyoung.dashboard.domain.pknu.repository.AssignmentRepository;
import com.junyoung.dashboard.domain.pknu.repository.CourseRepository;
import com.junyoung.dashboard.domain.pknu.repository.RawAssignmentRepository;
import com.junyoung.dashboard.domain.pknu.repository.SemesterRepository;
import com.junyoung.dashboard.domain.pknu.service.RawAssignmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// 실제 Kafka publish -> Consumer consume 흐름을 EmbeddedKafka로 검증한다.
// 리스너 자동 시작(test 프로파일 기본값 false)과 부트스트랩 서버(기본값 localhost:9092)를 이 임베디드 브로커 기준으로 오버라이드한다.
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=true",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(partitions = 1, topics = "pknu-raw-assignment")
@ActiveProfiles("test")
class RawAssignmentConsumerIntegrationTest {

    @Autowired
    private RawAssignmentService rawAssignmentService;

    @Autowired
    private RawAssignmentRepository rawAssignmentRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private RawAssignmentConsumer rawAssignmentConsumer;

    private Course createCourse() {
        Semester semester = semesterRepository.save(
                new Semester("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30)));
        return courseRepository.save(new Course(semester, "자료구조", "김교수", 3));
    }

    @Test
    void validRawAssignmentIsProcessedAndNormalizedIntoAssignment() {
        Course course = createCourse();
        long assignmentCountBefore = assignmentRepository.findByCourseId(course.getId(), Pageable.unpaged()).getTotalElements();

        var response = rawAssignmentService.create(
                new RawAssignmentRequest(course.getId().toString(), "1주차 과제", "2026-03-10", "false", "메모"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawAssignment raw = rawAssignmentRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.PROCESSED);
        });

        assertThat(assignmentRepository.findByCourseId(course.getId(), Pageable.unpaged()).getTotalElements())
                .isEqualTo(assignmentCountBefore + 1);
    }

    @Test
    void missingCourseMarksRawAssignmentAsFailedWithoutCreatingAssignment() {
        var response = rawAssignmentService.create(
                new RawAssignmentRequest("999999", "1주차 과제", "2026-03-10", "false", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawAssignment raw = rawAssignmentRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("courseId 999999 not found");
        });
    }

    @Test
    void unparsableDueDateMarksRawAssignmentAsFailedWithoutCreatingAssignment() {
        Course course = createCourse();

        var response = rawAssignmentService.create(
                new RawAssignmentRequest(course.getId().toString(), "1주차 과제", "not-a-date", "false", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawAssignment raw = rawAssignmentRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("dueDateRaw 파싱 실패");
        });
    }

    @Test
    void ambiguousCompletedRawMarksRawAssignmentAsFailedWithoutCreatingAssignment() {
        Course course = createCourse();

        var response = rawAssignmentService.create(
                new RawAssignmentRequest(course.getId().toString(), "1주차 과제", "2026-03-10", "yes", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            RawAssignment raw = rawAssignmentRepository.findById(response.id()).orElseThrow();
            assertThat(raw.getStatus()).isEqualTo(RawStatus.FAILED);
            assertThat(raw.getFailureReason()).contains("completedRaw가 true/false가 아님");
        });
    }

    @Test
    void redeliveredEventForAlreadyProcessedRawAssignmentDoesNotDuplicateAssignment() {
        Course course = createCourse();

        var response = rawAssignmentService.create(
                new RawAssignmentRequest(course.getId().toString(), "1주차 과제", "2026-03-10", "false", null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(rawAssignmentRepository.findById(response.id()).orElseThrow().getStatus())
                        .isEqualTo(RawStatus.PROCESSED));

        long assignmentCountAfterFirstProcessing = assignmentRepository
                .findByCourseId(course.getId(), Pageable.unpaged()).getTotalElements();

        // 이벤트 재전달 시나리오를 시뮬레이션 — 이미 PROCESSED인 레코드에 대해 컨슈머 로직을 한 번 더 직접 호출.
        rawAssignmentConsumer.consume(new RawAssignmentCreatedEvent(response.id()));

        assertThat(assignmentRepository.findByCourseId(course.getId(), Pageable.unpaged()).getTotalElements())
                .isEqualTo(assignmentCountAfterFirstProcessing);
    }
}
