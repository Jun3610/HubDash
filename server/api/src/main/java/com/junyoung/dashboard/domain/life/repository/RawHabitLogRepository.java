package com.junyoung.dashboard.domain.life.repository;

import com.junyoung.dashboard.domain.life.entity.RawHabitLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawHabitLogRepository extends JpaRepository<RawHabitLog, Long> {
}
