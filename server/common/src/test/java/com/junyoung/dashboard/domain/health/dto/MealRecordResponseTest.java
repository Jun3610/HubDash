package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.MealRecord;
import com.junyoung.dashboard.domain.health.entity.MealType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MealRecordResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        MealRecord record = new MealRecord(
                LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, 500,
                60.0, 20.0, 15.0, 300.0, "든든함");

        MealRecordResponse response = MealRecordResponse.from(record);

        assertThat(response.mealType()).isEqualTo(MealType.BREAKFAST);
        assertThat(response.calories()).isEqualTo(500);
        assertThat(response.carbsG()).isEqualTo(60.0);
        assertThat(response.sodiumMg()).isEqualTo(300.0);
    }
}
