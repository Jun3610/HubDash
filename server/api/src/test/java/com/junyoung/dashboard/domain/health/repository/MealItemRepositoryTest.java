package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.MealItem;
import com.junyoung.dashboard.domain.health.entity.MealRecord;
import com.junyoung.dashboard.domain.health.entity.MealType;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class MealItemRepositoryTest {

    @Autowired
    private MealRecordRepository mealRecordRepository;

    @Autowired
    private MealItemRepository mealItemRepository;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        MealRecord meal = mealRecordRepository.save(
                new MealRecord(LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, null));

        MealItem saved = mealItemRepository.save(new MealItem(meal, "밥", 300, 66.0, 5.0, 1.0, null));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findsOnlyItemsOfGivenMealRecord() {
        MealRecord breakfast = mealRecordRepository.save(
                new MealRecord(LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, null));
        MealRecord lunch = mealRecordRepository.save(
                new MealRecord(LocalDateTime.of(2026, 9, 1, 12, 0), MealType.LUNCH, null));
        mealItemRepository.save(new MealItem(breakfast, "밥", 300, null, null, null, null));
        mealItemRepository.save(new MealItem(breakfast, "계란", 150, null, null, null, null));
        mealItemRepository.save(new MealItem(lunch, "라면", 520, null, null, null, null));

        var page = mealItemRepository.findByMealRecordId(breakfast.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(MealItem::getName).containsExactlyInAnyOrder("밥", "계란");
    }
}
