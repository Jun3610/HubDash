package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.HealthLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;

public interface HealthLogRepository extends JpaRepository<HealthLog, Long> {

    long countByRecordedAtBetween(LocalDate weekStart, LocalDate weekEnd);

    // AVG는 null 값을 무시하고, 대상 행이 전부 null이면 null을 반환한다.
    @Query("SELECT AVG(h.weightKg) FROM HealthLog h WHERE h.recordedAt BETWEEN :weekStart AND :weekEnd")
    Double averageWeightKgBetween(LocalDate weekStart, LocalDate weekEnd);

    @Query("SELECT AVG(h.sleepHours) FROM HealthLog h WHERE h.recordedAt BETWEEN :weekStart AND :weekEnd")
    Double averageSleepHoursBetween(LocalDate weekStart, LocalDate weekEnd);
}
