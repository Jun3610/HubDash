package com.junyoung.dashboard.domain.pknu.service;

import com.junyoung.dashboard.domain.pknu.dto.AssignmentRequest;
import com.junyoung.dashboard.domain.pknu.dto.AssignmentResponse;
import com.junyoung.dashboard.domain.pknu.entity.Assignment;
import com.junyoung.dashboard.domain.pknu.entity.Course;
import com.junyoung.dashboard.domain.pknu.repository.AssignmentRepository;
import com.junyoung.dashboard.domain.pknu.repository.CourseRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;

    public AssignmentService(AssignmentRepository assignmentRepository, CourseRepository courseRepository) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public AssignmentResponse create(AssignmentRequest request) {
        Course course = getCourseOrThrow(request.courseId());
        Assignment saved = assignmentRepository.save(
                new Assignment(course, request.title(), request.dueDate(), request.completed(), request.notes()));
        return AssignmentResponse.from(saved);
    }

    public List<AssignmentResponse> findByCourseId(Long courseId) {
        return assignmentRepository.findByCourseId(courseId).stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    public AssignmentResponse findById(Long id) {
        return AssignmentResponse.from(getOrThrow(id));
    }

    @Transactional
    public AssignmentResponse update(Long id, AssignmentRequest request) {
        Assignment assignment = getOrThrow(id);
        Course course = getCourseOrThrow(request.courseId());
        assignment.update(course, request.title(), request.dueDate(), request.completed(), request.notes());
        return AssignmentResponse.from(assignment);
    }

    @Transactional
    public void delete(Long id) {
        assignmentRepository.delete(getOrThrow(id));
    }

    private Assignment getOrThrow(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(Assignment.class, id));
    }

    private Course getCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> EntityNotFoundException.of(Course.class, courseId));
    }
}
