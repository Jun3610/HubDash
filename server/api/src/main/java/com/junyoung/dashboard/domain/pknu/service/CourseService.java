package com.junyoung.dashboard.domain.pknu.service;

import com.junyoung.dashboard.domain.pknu.dto.CourseRequest;
import com.junyoung.dashboard.domain.pknu.dto.CourseResponse;
import com.junyoung.dashboard.domain.pknu.entity.Course;
import com.junyoung.dashboard.domain.pknu.entity.Semester;
import com.junyoung.dashboard.domain.pknu.repository.CourseRepository;
import com.junyoung.dashboard.domain.pknu.repository.SemesterRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Course course = new Course(semester, request.name(), request.professor(), request.credit());
        course.changeNotionUrl(request.notionUrl());
        course.changeGradeAndMemo(request.grade(), request.memo());
        course.changeTags(normalizeTags(request.tags()));
        Course saved = courseRepository.save(course);
        return CourseResponse.from(saved);
    }

    public Page<CourseResponse> findBySemesterId(Long semesterId, Pageable pageable) {
        return courseRepository.findBySemesterId(semesterId, pageable)
                .map(CourseResponse::from);
    }

    public CourseResponse findById(Long id) {
        return CourseResponse.from(getOrThrow(id));
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        Course course = getOrThrow(id);
        Semester semester = getSemesterOrThrow(request.semesterId());
        course.update(semester, request.name(), request.professor(), request.credit());
        course.changeNotionUrl(request.notionUrl());
        course.changeGradeAndMemo(request.grade(), request.memo());
        course.changeTags(normalizeTags(request.tags()));
        return CourseResponse.from(course);
    }

    @Transactional
    public void delete(Long id) {
        courseRepository.delete(getOrThrow(id));
    }

    private Course getOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(Course.class, id));
    }

    private Semester getSemesterOrThrow(Long semesterId) {
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> EntityNotFoundException.of(Semester.class, semesterId));
    }

    /** "  컴공, 시험 ,,컴공" → "컴공,시험" (공백 정리·빈 값·중복 제거), 없으면 null */
    static String normalizeTags(String tags) {
        if (tags == null) {
            return null;
        }
        String joined = java.util.Arrays.stream(tags.split(","))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }
}
