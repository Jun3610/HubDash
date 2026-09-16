package com.junyoung.dashboard.domain.life.repository;

import com.junyoung.dashboard.domain.life.entity.ReadingLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingLogRepository extends JpaRepository<ReadingLog, Long> {
}
