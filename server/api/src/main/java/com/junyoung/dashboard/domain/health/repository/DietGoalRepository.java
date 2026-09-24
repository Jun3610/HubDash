package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.DietGoal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DietGoalRepository extends JpaRepository<DietGoal, Long> {
}
