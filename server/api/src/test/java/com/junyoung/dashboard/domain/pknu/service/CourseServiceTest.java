package com.junyoung.dashboard.domain.pknu.service;

import com.junyoung.dashboard.domain.pknu.dto.CourseRequest;
import com.junyoung.dashboard.domain.pknu.dto.CourseResponse;
import com.junyoung.dashboard.domain.pknu.entity.Course;
import com.junyoung.dashboard.domain.pknu.entity.Semester;
import com.junyoung.dashboard.domain.pknu.repository.CourseRepository;
import com.junyoung.dashboard.domain.pknu.repository.SemesterRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SemesterRepository semesterRepository;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, semesterRepository);
    }

    @Test
    void createsCourseUnderExistingSemester() {
        Semester semester = new Semester("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));
        CourseRequest request = new CourseRequest(1L, "자료구조", "김교수", 3, "https://app.notion.com/p/abc");
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(courseRepository.save(any(Course.class)))
                .thenReturn(new Course(semester, "자료구조", "김교수", 3));

        CourseResponse response = courseService.create(request);

        assertThat(response.name()).isEqualTo("자료구조");

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertThat(captor.getValue().getSemester()).isSameAs(semester);
        assertThat(captor.getValue().getCredit()).isEqualTo(3);
        assertThat(captor.getValue().getNotionUrl()).isEqualTo("https://app.notion.com/p/abc");
    }

    @Test
    void throwsWhenSemesterMissingOnCreate() {
        CourseRequest request = new CourseRequest(1L, "자료구조", "김교수", 3, null);
        when(semesterRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.create(request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void movesCourseToNewSemesterOnUpdate() {
        Semester oldSemester = new Semester("2025-2학기", LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 20));
        Semester newSemester = new Semester("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));
        Course course = new Course(oldSemester, "자료구조", "김교수", 3);
        ReflectionTestUtils.setField(newSemester, "id", 2L);
        course.changeNotionUrl("https://app.notion.com/p/old");
        CourseRequest request = new CourseRequest(2L, "운영체제", "이교수", 4, "https://app.notion.com/p/new");
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(semesterRepository.findById(2L)).thenReturn(Optional.of(newSemester));

        courseService.update(10L, request);

        assertThat(course.getSemester()).isSameAs(newSemester);
        assertThat(course.getSemester()).isNotSameAs(oldSemester);
        assertThat(course.getName()).isEqualTo("운영체제");
        assertThat(course.getCredit()).isEqualTo(4);
        assertThat(course.getNotionUrl()).isEqualTo("https://app.notion.com/p/new");
    }

    @Test
    void throwsWhenNewSemesterMissingOnUpdate() {
        Semester oldSemester = new Semester("2025-2학기", LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 20));
        Course course = new Course(oldSemester, "자료구조", "김교수", 3);
        CourseRequest request = new CourseRequest(2L, "운영체제", "이교수", 4, null);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(semesterRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.update(10L, request))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
