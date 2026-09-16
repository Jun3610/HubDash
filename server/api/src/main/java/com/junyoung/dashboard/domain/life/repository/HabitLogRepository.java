package com.junyoung.dashboard.domain.life.repository;

import com.junyoung.dashboard.domain.life.entity.HabitLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {
    Page<HabitLog> findByHabitId(Long habitId, Pageable pageable);
}
