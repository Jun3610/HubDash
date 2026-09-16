package com.junyoung.dashboard.domain.pknu.dto;

import com.junyoung.dashboard.domain.pknu.entity.Assignment;
import com.junyoung.dashboard.domain.pknu.entity.Course;
import com.junyoung.dashboard.domain.pknu.entity.Semester;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentResponseTest {

    @Test
    void mapsEntityFieldsIncludingCourseId() {
        Semester semester = new Semester("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));
        Course course = new Course(semester, "자료구조", "김교수", 3);
        ReflectionTestUtils.setField(course, "id", 1L);
        Assignment assignment = new Assignment(course, "1주차 과제", LocalDate.of(2026, 3, 10), false, null);

        AssignmentResponse response = AssignmentResponse.from(assignment);

        assertThat(response.title()).isEqualTo("1주차 과제");
        assertThat(response.completed()).isFalse();
        assertThat(response.courseId()).isEqualTo(1L);
    }
}
