package com.junyoung.dashboard.domain.life.repository;

import com.junyoung.dashboard.domain.life.entity.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {
    List<HabitLog> findByHabitId(Long habitId);
}
