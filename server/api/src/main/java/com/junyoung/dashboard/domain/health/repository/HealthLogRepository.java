package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.HealthLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthLogRepository extends JpaRepository<HealthLog, Long> {
}
