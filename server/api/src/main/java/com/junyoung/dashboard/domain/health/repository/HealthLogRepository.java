package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.HealthLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface HealthLogRepository extends JpaRepository<HealthLog, Long> {

    // 기록 시각이 날짜+시각이라 주 범위는 [주 시작 00:00, 다음 주 시작 00:00)으로 센다 (이슈 #137)
    long countByRecordedAtGreaterThanEqualAndRecordedAtLessThan(LocalDateTime from, LocalDateTime to);

    // AVG는 null 값을 무시하고, 대상 행이 전부 null이면 null을 반환한다.
    @Query("SELECT AVG(h.weightKg) FROM HealthLog h WHERE h.recordedAt >= :from AND h.recordedAt < :to")
    Double averageWeightKgBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT AVG(h.sleepHours) FROM HealthLog h WHERE h.recordedAt >= :from AND h.recordedAt < :to")
    Double averageSleepHoursBetween(LocalDateTime from, LocalDateTime to);
}
