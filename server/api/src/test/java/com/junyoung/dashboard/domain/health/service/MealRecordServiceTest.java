package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.DailyMealSummaryResponse;
import com.junyoung.dashboard.domain.health.dto.DailyMealSummaryResponse.MealTypeSummary;
import com.junyoung.dashboard.domain.health.dto.MealRecordRequest;
import com.junyoung.dashboard.domain.health.dto.MealRecordResponse;
import com.junyoung.dashboard.domain.health.dto.MealTotals;
import com.junyoung.dashboard.domain.health.entity.MealItem;
import com.junyoung.dashboard.domain.health.entity.MealRecord;
import com.junyoung.dashboard.domain.health.entity.MealType;
import com.junyoung.dashboard.domain.health.repository.MealRecordRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MealRecordServiceTest {

    @Mock
    private MealRecordRepository mealRecordRepository;

    private MealRecordService mealRecordService;

    @BeforeEach
    void setUp() {
        mealRecordService = new MealRecordService(mealRecordRepository);
    }

    @Test
    void createsMealRecordUsingRequestFields() {
        MealRecordRequest request = new MealRecordRequest(LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, null);
        when(mealRecordRepository.save(any(MealRecord.class)))
                .thenReturn(new MealRecord(LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, null));

        MealRecordResponse response = mealRecordService.create(request);

        assertThat(response.mealType()).isEqualTo(MealType.BREAKFAST);
        assertThat(response.items()).isEmpty();

        ArgumentCaptor<MealRecord> captor = ArgumentCaptor.forClass(MealRecord.class);
        verify(mealRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getMealType()).isEqualTo(request.mealType());
    }

    @Test
    void throwsWhenMealRecordNotFound() {
        when(mealRecordRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mealRecordService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void updatesEveryFieldIncludingMealType() {
        MealRecord existing = new MealRecord(LocalDateTime.of(2026, 1, 1, 8, 0), MealType.BREAKFAST, "기존");
        when(mealRecordRepository.findById(1L)).thenReturn(Optional.of(existing));

        MealRecordResponse response = mealRecordService.update(1L,
                new MealRecordRequest(LocalDateTime.of(2026, 1, 1, 19, 0), MealType.DINNER, "새 기록"));

        assertThat(response.mealType()).isEqualTo(MealType.DINNER);
        assertThat(response.consumedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 19, 0));
        assertThat(response.notes()).isEqualTo("새 기록");
    }

    @Test
    void dailySummaryTotalsAcrossMealsAndSplitsByMealType() {
        LocalDate date = LocalDate.of(2026, 9, 19);
        MealRecord breakfast = mealWith(MealType.BREAKFAST, 8,
                new Item("밥", 300, 66.0, 5.0, 1.0), new Item("계란", 150, 1.0, 12.0, 10.0));
        MealRecord lunch = mealWith(MealType.LUNCH, 12, new Item("라면", 520, 83.0, 11.0, 16.0));
        MealRecord snack1 = mealWith(MealType.SNACK, 15, new Item("바나나", 105, 27.0, 1.3, 0.4));
        MealRecord snack2 = mealWith(MealType.SNACK, 20, new Item("우유", 120, 10.0, 6.0, 6.0));
        when(mealRecordRepository.findByConsumedAtGreaterThanEqualAndConsumedAtLessThan(
                date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(breakfast, lunch, snack1, snack2));

        DailyMealSummaryResponse summary = mealRecordService.dailySummary(date);

        assertThat(summary.date()).isEqualTo(date);
        assertThat(summary.totals().calories()).isEqualTo(300 + 150 + 520 + 105 + 120);
        assertThat(summary.totals().carbsG()).isEqualTo(187.0);
        assertThat(summary.totals().proteinG()).isEqualTo(35.3);
        assertThat(summary.totals().fatG()).isEqualTo(33.4);

        // 끼니 종류는 항상 아침/점심/저녁/간식 순서로 4개
        assertThat(summary.meals()).extracting(MealTypeSummary::mealType)
                .containsExactly(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK);
        assertThat(summary.meals().get(0).totals().calories()).isEqualTo(450);
        assertThat(summary.meals().get(0).itemCount()).isEqualTo(2);
        assertThat(summary.meals().get(1).totals().calories()).isEqualTo(520);
        // 같은 종류의 끼니가 여러 개면(간식 2번) 합쳐서 집계
        assertThat(summary.meals().get(3).totals().calories()).isEqualTo(225);
        assertThat(summary.meals().get(3).itemCount()).isEqualTo(2);
    }

    @Test
    void dailySummaryForEmptyDayHasZeroTotalsAndAllFourMealTypes() {
        LocalDate date = LocalDate.of(2026, 9, 19);
        when(mealRecordRepository.findByConsumedAtGreaterThanEqualAndConsumedAtLessThan(any(), any()))
                .thenReturn(List.of());

        DailyMealSummaryResponse summary = mealRecordService.dailySummary(date);

        assertThat(summary.totals()).isEqualTo(new MealTotals(0, 0.0, 0.0, 0.0, 0.0));
        assertThat(summary.meals()).hasSize(4);
        assertThat(summary.meals()).allSatisfy(meal -> {
            assertThat(meal.itemCount()).isZero();
            assertThat(meal.totals().calories()).isZero();
        });
    }

    @Test
    void dailySummaryQueriesHalfOpenRangeSoMidnightBelongsToTheNextDay() {
        LocalDate date = LocalDate.of(2026, 9, 19);
        when(mealRecordRepository.findByConsumedAtGreaterThanEqualAndConsumedAtLessThan(any(), any()))
                .thenReturn(List.of());

        mealRecordService.dailySummary(date);

        verify(mealRecordRepository).findByConsumedAtGreaterThanEqualAndConsumedAtLessThan(
                LocalDateTime.of(2026, 9, 19, 0, 0), LocalDateTime.of(2026, 9, 20, 0, 0));
    }

    private record Item(String name, int calories, double carbs, double protein, double fat) {
    }

    private MealRecord mealWith(MealType type, int hour, Item... items) {
        MealRecord record = new MealRecord(LocalDateTime.of(2026, 9, 19, hour, 0), type, null);
        for (Item item : items) {
            record.getItems().add(new MealItem(record, item.name(), item.calories(),
                    item.carbs(), item.protein(), item.fat(), null));
        }
        return record;
    }
}
