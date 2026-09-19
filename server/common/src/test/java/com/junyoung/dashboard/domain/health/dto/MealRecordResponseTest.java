package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.MealItem;
import com.junyoung.dashboard.domain.health.entity.MealRecord;
import com.junyoung.dashboard.domain.health.entity.MealType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MealRecordResponseTest {

    @Test
    void mapsEntityFieldsAndComputesTotalsFromItems() {
        MealRecord record = new MealRecord(LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, "든든함");
        record.getItems().add(new MealItem(record, "밥", 300, 66.0, 5.0, 1.0, 2.0));
        record.getItems().add(new MealItem(record, "계란", 150, 1.0, 12.0, 10.0, 140.0));

        MealRecordResponse response = MealRecordResponse.from(record);

        assertThat(response.mealType()).isEqualTo(MealType.BREAKFAST);
        assertThat(response.notes()).isEqualTo("든든함");
        assertThat(response.items()).extracting(MealItemResponse::name).containsExactly("밥", "계란");
        assertThat(response.totals().calories()).isEqualTo(450);
        assertThat(response.totals().carbsG()).isEqualTo(67.0);
        assertThat(response.totals().proteinG()).isEqualTo(17.0);
        assertThat(response.totals().fatG()).isEqualTo(11.0);
        assertThat(response.totals().sodiumMg()).isEqualTo(142.0);
    }

    @Test
    void mealWithoutItemsHasZeroTotals() {
        MealRecord record = new MealRecord(LocalDateTime.of(2026, 9, 1, 8, 0), MealType.SNACK, null);

        MealRecordResponse response = MealRecordResponse.from(record);

        assertThat(response.items()).isEmpty();
        assertThat(response.totals()).isEqualTo(new MealTotals(0, 0.0, 0.0, 0.0, 0.0));
    }
}
