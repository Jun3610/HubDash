package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.MealRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealRecordRepository extends JpaRepository<MealRecord, Long> {
}
