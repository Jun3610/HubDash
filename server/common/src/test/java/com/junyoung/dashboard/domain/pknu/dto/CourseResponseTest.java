package com.junyoung.dashboard.domain.pknu.dto;

import com.junyoung.dashboard.domain.pknu.entity.Course;
import com.junyoung.dashboard.domain.pknu.entity.Semester;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CourseResponseTest {

    @Test
    void mapsEntityFieldsIncludingSemesterId() {
        Semester semester = new Semester("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));
        ReflectionTestUtils.setField(semester, "id", 1L);
        Course course = new Course(semester, "자료구조", "김교수", 3);

        CourseResponse response = CourseResponse.from(course);

        assertThat(response.name()).isEqualTo("자료구조");
        assertThat(response.credit()).isEqualTo(3);
        assertThat(response.semesterId()).isEqualTo(1L);
    }
}
