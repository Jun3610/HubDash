package com.junyoung.dashboard.domain.pknu.repository;

import com.junyoung.dashboard.domain.pknu.entity.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    Page<Assignment> findByCourseId(Long courseId, Pageable pageable);

    @Query("SELECT DISTINCT a.course.id FROM Assignment a WHERE a.dueDate BETWEEN :weekStart AND :weekEnd")
    List<Long> findDistinctCourseIdsWithDueBetween(LocalDate weekStart, LocalDate weekEnd);

    long countByCourseIdAndDueDateBetween(Long courseId, LocalDate weekStart, LocalDate weekEnd);

    long countByCourseIdAndDueDateBetweenAndCompletedTrue(Long courseId, LocalDate weekStart, LocalDate weekEnd);
}
