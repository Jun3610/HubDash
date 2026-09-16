package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.RawHealthLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawHealthLogRepository extends JpaRepository<RawHealthLog, Long> {
}
