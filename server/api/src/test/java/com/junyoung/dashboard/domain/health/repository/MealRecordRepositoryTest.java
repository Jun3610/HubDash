package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.MealItem;
import com.junyoung.dashboard.domain.health.entity.MealRecord;
import com.junyoung.dashboard.domain.health.entity.MealType;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class MealRecordRepositoryTest {

    @Autowired
    private MealRecordRepository mealRecordRepository;

    @Autowired
    private MealItemRepository mealItemRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        MealRecord saved = mealRecordRepository.save(
                new MealRecord(LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, null));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findsMealsWithinHalfOpenDayRange() {
        LocalDateTime dayStart = LocalDateTime.of(2026, 9, 19, 0, 0);
        mealRecordRepository.save(new MealRecord(dayStart.minusSeconds(1), MealType.DINNER, "전날 23:59:59"));
        mealRecordRepository.save(new MealRecord(dayStart, MealType.BREAKFAST, "당일 00:00 포함"));
        mealRecordRepository.save(new MealRecord(dayStart.plusHours(12), MealType.LUNCH, "당일 낮"));
        mealRecordRepository.save(new MealRecord(dayStart.plusDays(1), MealType.BREAKFAST, "다음날 00:00 제외"));

        List<MealRecord> found = mealRecordRepository
                .findByConsumedAtGreaterThanEqualAndConsumedAtLessThan(dayStart, dayStart.plusDays(1));

        assertThat(found).extracting(MealRecord::getNotes).containsExactlyInAnyOrder("당일 00:00 포함", "당일 낮");
    }

    @Test
    void deletingMealCascadesToItsItems() {
        MealRecord meal = mealRecordRepository.save(
                new MealRecord(LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, null));
        MealItem item = mealItemRepository.save(new MealItem(meal, "밥", 300, 66.0, 5.0, 1.0, null));
        meal.getItems().add(item);
        entityManager.flush();
        entityManager.clear();

        mealRecordRepository.delete(mealRecordRepository.findById(meal.getId()).orElseThrow());
        entityManager.flush();

        assertThat(mealItemRepository.findById(item.getId())).isEmpty();
    }

    @Test
    void loadsItemsOfMeal() {
        MealRecord meal = mealRecordRepository.save(
                new MealRecord(LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, null));
        mealItemRepository.save(new MealItem(meal, "밥", 300, 66.0, 5.0, 1.0, null));
        mealItemRepository.save(new MealItem(meal, "계란", 150, 1.0, 12.0, 10.0, null));
        entityManager.flush();
        entityManager.clear();

        MealRecord reloaded = mealRecordRepository.findById(meal.getId()).orElseThrow();

        assertThat(reloaded.getItems()).extracting(MealItem::getName).containsExactlyInAnyOrder("밥", "계란");
    }
}
