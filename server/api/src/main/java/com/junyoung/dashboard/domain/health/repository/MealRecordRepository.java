package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.MealRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MealRecordRepository extends JpaRepository<MealRecord, Long> {

    // [from, to) 범위 — 하루 합계는 그날 00:00 이상, 다음 날 00:00 미만.
    List<MealRecord> findByConsumedAtGreaterThanEqualAndConsumedAtLessThan(LocalDateTime from, LocalDateTime to);
}
