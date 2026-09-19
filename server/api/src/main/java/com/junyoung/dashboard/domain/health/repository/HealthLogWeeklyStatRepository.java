package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.HealthLogWeeklyStat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface HealthLogWeeklyStatRepository extends JpaRepository<HealthLogWeeklyStat, Long> {
    Optional<HealthLogWeeklyStat> findByWeekStart(LocalDate weekStart);

    Page<HealthLogWeeklyStat> findAllByOrderByWeekStartDesc(Pageable pageable);
}
