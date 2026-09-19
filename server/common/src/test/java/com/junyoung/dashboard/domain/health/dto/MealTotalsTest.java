package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.MealItem;
import com.junyoung.dashboard.domain.health.entity.MealRecord;
import com.junyoung.dashboard.domain.health.entity.MealType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MealTotalsTest {

    private final MealRecord meal = new MealRecord(LocalDateTime.of(2026, 9, 1, 8, 0), MealType.LUNCH, null);

    @Test
    void treatsMissingMacrosAsZeroButStillCountsCalories() {
        // 탄단지를 모르는 음식도 칼로리는 합계에 들어가야 한다.
        MealTotals totals = MealTotals.of(List.of(
                new MealItem(meal, "라면", 520, 83.0, 11.0, 16.0, 1970.0),
                new MealItem(meal, "김치", 15, null, null, null, null)));

        assertThat(totals.calories()).isEqualTo(535);
        assertThat(totals.carbsG()).isEqualTo(83.0);
        assertThat(totals.proteinG()).isEqualTo(11.0);
        assertThat(totals.fatG()).isEqualTo(16.0);
        assertThat(totals.sodiumMg()).isEqualTo(1970.0);
    }

    @Test
    void roundsToAvoidFloatingPointNoise() {
        // 0.1 + 0.2 = 0.30000000000000004
        MealTotals totals = MealTotals.of(List.of(
                new MealItem(meal, "a", 1, 0.1, 0.1, 0.1, 0.1),
                new MealItem(meal, "b", 1, 0.2, 0.2, 0.2, 0.2)));

        assertThat(totals.carbsG()).isEqualTo(0.3);
        assertThat(totals.proteinG()).isEqualTo(0.3);
    }

    @Test
    void emptyItemsAreAllZero() {
        assertThat(MealTotals.of(List.of())).isEqualTo(new MealTotals(0, 0.0, 0.0, 0.0, 0.0));
    }
}
