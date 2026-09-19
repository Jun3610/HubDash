package com.junyoung.dashboard.domain.pknu.repository;

import com.junyoung.dashboard.domain.pknu.entity.AssignmentWeeklyStat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AssignmentWeeklyStatRepository extends JpaRepository<AssignmentWeeklyStat, Long> {
    Optional<AssignmentWeeklyStat> findByCourseIdAndWeekStart(Long courseId, LocalDate weekStart);

    Page<AssignmentWeeklyStat> findByCourseId(Long courseId, Pageable pageable);
}
