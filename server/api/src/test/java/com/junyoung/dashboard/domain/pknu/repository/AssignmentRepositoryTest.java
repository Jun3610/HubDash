package com.junyoung.dashboard.domain.pknu.repository;

import com.junyoung.dashboard.domain.pknu.entity.Assignment;
import com.junyoung.dashboard.domain.pknu.entity.Course;
import com.junyoung.dashboard.domain.pknu.entity.Semester;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class AssignmentRepositoryTest {

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Test
    void findsAssignmentsByCourseId() {
        Semester semester = semesterRepository.save(
                new Semester("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30)));
        Course course = courseRepository.save(new Course(semester, "자료구조", "김교수", 3));
        assignmentRepository.save(new Assignment(course, "1주차 과제", LocalDate.of(2026, 3, 10), false, null));

        Page<Assignment> assignments = assignmentRepository.findByCourseId(course.getId(), Pageable.unpaged());

        assertThat(assignments.getContent()).hasSize(1);
        assertThat(assignments.getContent().get(0).getTitle()).isEqualTo("1주차 과제");
    }
}
