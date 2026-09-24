package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.Food;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRepository extends JpaRepository<Food, Long> {
}
