package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.MealItemRequest;
import com.junyoung.dashboard.domain.health.dto.MealItemResponse;
import com.junyoung.dashboard.domain.health.entity.MealItem;
import com.junyoung.dashboard.domain.health.entity.MealRecord;
import com.junyoung.dashboard.domain.health.entity.MealType;
import com.junyoung.dashboard.domain.health.repository.MealItemRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MealItemServiceTest {

    @Mock
    private MealItemRepository mealItemRepository;

    @Mock
    private MealRecordRepository mealRecordRepository;

    private MealItemService mealItemService;

    private final MealRecord breakfast = new MealRecord(LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, null);
    private final MealRecord dinner = new MealRecord(LocalDateTime.of(2026, 9, 1, 19, 0), MealType.DINNER, null);

    @BeforeEach
    void setUp() {
        mealItemService = new MealItemService(mealItemRepository, mealRecordRepository);
    }

    @Test
    void createsItemAttachedToMealRecord() {
        when(mealRecordRepository.findById(1L)).thenReturn(Optional.of(breakfast));
        when(mealItemRepository.save(any(MealItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MealItemResponse response = mealItemService.create(
                new MealItemRequest(1L, "신라면", 520, 83.0, 11.0, 16.0, 1970.0));

        assertThat(response.name()).isEqualTo("신라면");
        assertThat(response.calories()).isEqualTo(520);

        ArgumentCaptor<MealItem> captor = ArgumentCaptor.forClass(MealItem.class);
        verify(mealItemRepository).save(captor.capture());
        assertThat(captor.getValue().getMealRecord()).isSameAs(breakfast);
    }

    @Test
    void createFailsWhenMealRecordDoesNotExist() {
        when(mealRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mealItemService.create(new MealItemRequest(99L, "밥", 300, null, null, null, null)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
        verify(mealItemRepository, never()).save(any());
    }

    @Test
    void throwsWhenItemNotFound() {
        when(mealItemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mealItemService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void updateCanMoveItemToAnotherMealAndChangesEveryField() {
        MealItem existing = new MealItem(breakfast, "밥", 300, 66.0, 5.0, 1.0, 2.0);
        when(mealItemRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(mealRecordRepository.findById(2L)).thenReturn(Optional.of(dinner));

        MealItemResponse response = mealItemService.update(1L,
                new MealItemRequest(2L, "현미밥", 280, 60.0, 6.0, 2.0, 1.0));

        assertThat(existing.getMealRecord()).isSameAs(dinner);
        assertThat(response.name()).isEqualTo("현미밥");
        assertThat(response.calories()).isEqualTo(280);
        assertThat(response.carbsG()).isEqualTo(60.0);
        assertThat(response.proteinG()).isEqualTo(6.0);
        assertThat(response.fatG()).isEqualTo(2.0);
        assertThat(response.sodiumMg()).isEqualTo(1.0);
    }

    @Test
    void updateFailsWhenTargetMealRecordDoesNotExist() {
        when(mealItemRepository.findById(1L))
                .thenReturn(Optional.of(new MealItem(breakfast, "밥", 300, null, null, null, null)));
        when(mealRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mealItemService.update(1L, new MealItemRequest(99L, "밥", 300, null, null, null, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
