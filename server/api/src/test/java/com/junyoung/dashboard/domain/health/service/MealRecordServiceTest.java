package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.MealRecordRequest;
import com.junyoung.dashboard.domain.health.dto.MealRecordResponse;
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

import java.time.LocalDateTime;
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
        MealRecordRequest request = new MealRecordRequest(
                LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, 500, 60.0, 20.0, 15.0, 300.0, null);
        when(mealRecordRepository.save(any(MealRecord.class)))
                .thenReturn(new MealRecord(LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, 500,
                        60.0, 20.0, 15.0, 300.0, null));

        MealRecordResponse response = mealRecordService.create(request);

        assertThat(response.mealType()).isEqualTo(MealType.BREAKFAST);

        ArgumentCaptor<MealRecord> captor = ArgumentCaptor.forClass(MealRecord.class);
        verify(mealRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getCalories()).isEqualTo(request.calories());
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
        MealRecord existing = new MealRecord(
                LocalDateTime.of(2026, 1, 1, 8, 0), MealType.BREAKFAST, 400, 50.0, 15.0, 10.0, 200.0, "기존");
        when(mealRecordRepository.findById(1L)).thenReturn(Optional.of(existing));

        MealRecordRequest request = new MealRecordRequest(
                LocalDateTime.of(2026, 1, 1, 19, 0), MealType.DINNER, 700, 80.0, 30.0, 25.0, 500.0, "새 기록");

        MealRecordResponse response = mealRecordService.update(1L, request);

        assertThat(response.mealType()).isEqualTo(MealType.DINNER);
        assertThat(response.calories()).isEqualTo(700);
        assertThat(response.sodiumMg()).isEqualTo(500.0);
        assertThat(response.notes()).isEqualTo("새 기록");
    }
}
