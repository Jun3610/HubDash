package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.MealItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface MealItemRepository extends JpaRepository<MealItem, Long> {
    Page<MealItem> findByMealRecordId(Long mealRecordId, Pageable pageable);

    // [from, to) 범위에 먹은 끼니에 딸린 음식 항목 수 — 주간 집계 Reader가 "그 주에 기록이 있는지" 판단할 때 쓴다.
    @Query("SELECT COUNT(i) FROM MealItem i WHERE i.mealRecord.consumedAt >= :from AND i.mealRecord.consumedAt < :to")
    long countByConsumedAtBetween(LocalDateTime from, LocalDateTime to);
}
