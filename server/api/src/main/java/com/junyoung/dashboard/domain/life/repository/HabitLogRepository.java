package com.junyoung.dashboard.domain.life.repository;

import com.junyoung.dashboard.domain.life.entity.HabitLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {
    Page<HabitLog> findByHabitId(Long habitId, Pageable pageable);

    @Query("SELECT DISTINCT l.habit.id FROM HabitLog l WHERE l.performedAt BETWEEN :weekStart AND :weekEnd")
    List<Long> findDistinctHabitIdsWithLogsBetween(LocalDate weekStart, LocalDate weekEnd);

    long countByHabitIdAndPerformedAtBetween(Long habitId, LocalDate weekStart, LocalDate weekEnd);

    long countByHabitIdAndPerformedAtBetweenAndCompletedTrue(Long habitId, LocalDate weekStart, LocalDate weekEnd);
}
