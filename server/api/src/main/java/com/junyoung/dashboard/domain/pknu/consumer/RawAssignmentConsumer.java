package com.junyoung.dashboard.domain.pknu.consumer;

import com.junyoung.dashboard.domain.pknu.entity.Assignment;
import com.junyoung.dashboard.domain.pknu.entity.Course;
import com.junyoung.dashboard.domain.pknu.entity.RawAssignment;
import com.junyoung.dashboard.domain.pknu.entity.RawStatus;
import com.junyoung.dashboard.domain.pknu.event.RawAssignmentCreatedEvent;
import com.junyoung.dashboard.domain.pknu.repository.AssignmentRepository;
import com.junyoung.dashboard.domain.pknu.repository.CourseRepository;
import com.junyoung.dashboard.domain.pknu.repository.RawAssignmentRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// 검증 실패는 "일시적 장애"가 아니라 "비즈니스 실패"다 — 예외를 던져 Kafka가 무한 재시도/재배달하게 만들지 않고,
// RawAssignment.status를 FAILED로 명시적으로 기록한 뒤 정상 종료(ack)한다.
// 부모(Course) 존재 확인도 같은 원칙 — EntityNotFoundException을 던지지 않고 findById 결과를 직접 체크한다.
// Assignment는 Course의 자식이고 Course는 Semester의 자식(2단계 체인)이지만, Assignment 생성 시엔
// courseId 존재만 확인하면 충분하다 — Course가 어느 Semester 소속인지는 이미 Course 엔티티에 저장돼 있다.
@Component
public class RawAssignmentConsumer {

    private static final Logger log = LoggerFactory.getLogger(RawAssignmentConsumer.class);

    private final RawAssignmentRepository rawAssignmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;

    public RawAssignmentConsumer(RawAssignmentRepository rawAssignmentRepository,
                                  AssignmentRepository assignmentRepository,
                                  CourseRepository courseRepository) {
        this.rawAssignmentRepository = rawAssignmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
    }

    @KafkaListener(topics = KafkaTopicConfig.PKNU_RAW_ASSIGNMENT_TOPIC)
    @Transactional
    public void consume(RawAssignmentCreatedEvent event) {
        RawAssignment raw = rawAssignmentRepository.findById(event.rawAssignmentId()).orElse(null);
        if (raw == null) {
            log.warn("RawAssignment {} not found, skipping", event.rawAssignmentId());
            return;
        }
        // Kafka는 at-least-once 전달을 보장한다 — 크래시/리밸런싱으로 같은 이벤트가 재전달될 수 있으므로,
        // 이미 처리 끝난(PROCESSED/FAILED) 레코드는 중복 처리(Assignment 중복 생성)하지 않도록 건너뛴다.
        if (raw.getStatus() != RawStatus.PENDING) {
            log.info("RawAssignment {} already in status {}, skipping duplicate delivery", raw.getId(), raw.getStatus());
            return;
        }

        Long courseId;
        try {
            courseId = Long.parseLong(raw.getCourseIdRaw());
        } catch (NumberFormatException e) {
            raw.markFailed("courseIdRaw 파싱 실패: " + raw.getCourseIdRaw());
            return;
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            raw.markFailed("courseId " + courseId + " not found");
            return;
        }

        LocalDate dueDate;
        try {
            dueDate = LocalDate.parse(raw.getDueDateRaw());
        } catch (DateTimeParseException e) {
            raw.markFailed("dueDateRaw 파싱 실패: " + raw.getDueDateRaw());
            return;
        }

        // reminder/life 확장 교훈: Boolean.parseBoolean()은 "yes"/"1" 같은 모호한 값을 조용히 false로 취급한다 —
        // "true"/"false" 문자열만 엄격하게 허용하고, 그 외는 명시적으로 FAILED 처리한다.
        Boolean completed;
        if ("true".equals(raw.getCompletedRaw())) {
            completed = Boolean.TRUE;
        } else if ("false".equals(raw.getCompletedRaw())) {
            completed = Boolean.FALSE;
        } else {
            raw.markFailed("completedRaw가 true/false가 아님: " + raw.getCompletedRaw());
            return;
        }

        assignmentRepository.save(new Assignment(course, raw.getTitleRaw(), dueDate, completed, raw.getNotes()));
        raw.markProcessed();
    }
}
