package com.junyoung.dashboard.domain.pknu.service;

import com.junyoung.dashboard.domain.pknu.dto.AssignmentRequest;
import com.junyoung.dashboard.domain.pknu.dto.AssignmentResponse;
import com.junyoung.dashboard.domain.pknu.entity.Assignment;
import com.junyoung.dashboard.domain.pknu.entity.Course;
import com.junyoung.dashboard.domain.pknu.entity.Semester;
import com.junyoung.dashboard.domain.pknu.repository.AssignmentRepository;
import com.junyoung.dashboard.domain.pknu.repository.CourseRepository;
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
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private CourseRepository courseRepository;

    private AssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentService(assignmentRepository, courseRepository);
    }

    private Semester semester() {
        return new Semester("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));
    }

    @Test
    void createsAssignmentUnderExistingCourse() {
        Course course = new Course(semester(), "자료구조", "김교수", 3);
        AssignmentRequest request = new AssignmentRequest(1L, "1주차 과제", LocalDate.of(2026, 3, 10), false, null);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(assignmentRepository.save(any(Assignment.class)))
                .thenReturn(new Assignment(course, "1주차 과제", LocalDate.of(2026, 3, 10), false, null));

        AssignmentResponse response = assignmentService.create(request);

        assertThat(response.title()).isEqualTo("1주차 과제");

        ArgumentCaptor<Assignment> captor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getCourse()).isSameAs(course);
        assertThat(captor.getValue().getCompleted()).isFalse();
    }

    @Test
    void throwsWhenCourseMissingOnCreate() {
        AssignmentRequest request = new AssignmentRequest(1L, "1주차 과제", LocalDate.of(2026, 3, 10), false, null);
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.create(request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void movesAssignmentToNewCourseOnUpdate() {
        Course oldCourse = new Course(semester(), "자료구조", "김교수", 3);
        Course newCourse = new Course(semester(), "운영체제", "이교수", 4);
        ReflectionTestUtils.setField(newCourse, "id", 2L);
        Assignment assignment = new Assignment(oldCourse, "1주차 과제", LocalDate.of(2026, 3, 10), false, null);
        AssignmentRequest request = new AssignmentRequest(2L, "2주차 과제", LocalDate.of(2026, 3, 17), true, "이동됨");
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(newCourse));

        assignmentService.update(10L, request);

        assertThat(assignment.getCourse()).isSameAs(newCourse);
        assertThat(assignment.getCourse()).isNotSameAs(oldCourse);
        assertThat(assignment.getCompleted()).isTrue();
    }

    @Test
    void throwsWhenNewCourseMissingOnUpdate() {
        Course oldCourse = new Course(semester(), "자료구조", "김교수", 3);
        Assignment assignment = new Assignment(oldCourse, "1주차 과제", LocalDate.of(2026, 3, 10), false, null);
        AssignmentRequest request = new AssignmentRequest(2L, "2주차 과제", LocalDate.of(2026, 3, 17), true, null);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(courseRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.update(10L, request))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
