package com.junyoung.dashboard.domain.pknu.service;

import com.junyoung.dashboard.domain.pknu.dto.CourseRequest;
import com.junyoung.dashboard.domain.pknu.dto.CourseResponse;
import com.junyoung.dashboard.domain.pknu.entity.Course;
import com.junyoung.dashboard.domain.pknu.entity.Semester;
import com.junyoung.dashboard.domain.pknu.repository.CourseRepository;
import com.junyoung.dashboard.domain.pknu.repository.SemesterRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;

    public CourseService(CourseRepository courseRepository, SemesterRepository semesterRepository) {
        this.courseRepository = courseRepository;
        this.semesterRepository = semesterRepository;
    }

    @Transactional
    public CourseResponse create(CourseRequest request) {
        Semester semester = getSemesterOrThrow(request.semesterId());
        Course saved = courseRepository.save(
                new Course(semester, request.name(), request.professor(), request.credit()));
        return CourseResponse.from(saved);
    }

    public List<CourseResponse> findBySemesterId(Long semesterId) {
        return courseRepository.findBySemesterId(semesterId).stream()
                .map(CourseResponse::from)
                .toList();
    }

    public CourseResponse findById(Long id) {
        return CourseResponse.from(getOrThrow(id));
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        Course course = getOrThrow(id);
        Semester semester = getSemesterOrThrow(request.semesterId());
        course.update(semester, request.name(), request.professor(), request.credit());
        return CourseResponse.from(course);
    }

    @Transactional
    public void delete(Long id) {
        courseRepository.delete(getOrThrow(id));
    }

    private Course getOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("course " + id + " not found"));
    }

    private Semester getSemesterOrThrow(Long semesterId) {
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> new EntityNotFoundException("semester " + semesterId + " not found"));
    }
}
