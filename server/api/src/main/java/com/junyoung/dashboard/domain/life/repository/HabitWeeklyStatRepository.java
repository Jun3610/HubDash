package com.junyoung.dashboard.domain.life.repository;

import com.junyoung.dashboard.domain.life.entity.HabitWeeklyStat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface HabitWeeklyStatRepository extends JpaRepository<HabitWeeklyStat, Long> {
    Optional<HabitWeeklyStat> findByHabitIdAndWeekStart(Long habitId, LocalDate weekStart);

    Page<HabitWeeklyStat> findByHabitId(Long habitId, Pageable pageable);
}
