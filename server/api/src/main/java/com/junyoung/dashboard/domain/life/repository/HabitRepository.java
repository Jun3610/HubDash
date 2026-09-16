package com.junyoung.dashboard.domain.life.repository;

import com.junyoung.dashboard.domain.life.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitRepository extends JpaRepository<Habit, Long> {
}
