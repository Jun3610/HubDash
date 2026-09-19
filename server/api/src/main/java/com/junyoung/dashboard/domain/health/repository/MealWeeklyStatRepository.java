package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.MealWeeklyStat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MealWeeklyStatRepository extends JpaRepository<MealWeeklyStat, Long> {
    Optional<MealWeeklyStat> findByWeekStart(LocalDate weekStart);

    Page<MealWeeklyStat> findAllByOrderByWeekStartDesc(Pageable pageable);
}
