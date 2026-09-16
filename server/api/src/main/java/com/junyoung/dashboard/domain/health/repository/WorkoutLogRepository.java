package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.WorkoutLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, Long> {
}
