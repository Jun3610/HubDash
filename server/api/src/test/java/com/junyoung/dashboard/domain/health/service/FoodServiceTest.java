package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.FoodRequest;
import com.junyoung.dashboard.domain.health.dto.FoodResponse;
import com.junyoung.dashboard.domain.health.entity.Food;
import com.junyoung.dashboard.domain.health.repository.FoodRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodServiceTest {

    @Mock
    private FoodRepository foodRepository;

    private FoodService foodService;

    @BeforeEach
    void setUp() {
        foodService = new FoodService(foodRepository);
    }

    @Test
    void createsFoodAndTreatsMissingPinnedAsFalse() {
        FoodRequest request = new FoodRequest("바나나", 100, 27.0, 0.3, 1.3, "1개", null);
        when(foodRepository.save(any(Food.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FoodResponse response = foodService.create(request);

        assertThat(response.name()).isEqualTo("바나나");
        assertThat(response.pinned()).isFalse();
        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodRepository).save(captor.capture());
        assertThat(captor.getValue().getCarbsG()).isEqualTo(27.0);
        assertThat(captor.getValue().getFatG()).isEqualTo(0.3);
        assertThat(captor.getValue().getProteinG()).isEqualTo(1.3);
    }

    @Test
    void listsPinnedFirstThenByNameWhenNoSortGiven() {
        when(foodRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        foodService.findAll(PageRequest.of(0, 20));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(foodRepository).findAll(captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(FoodService.DEFAULT_SORT);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void keepsSortWhenCallerGivesOne() {
        when(foodRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        PageRequest byCalories = PageRequest.of(0, 20, Sort.by("calories"));

        foodService.findAll(byCalories);

        verify(foodRepository).findAll(byCalories);
    }

    @Test
    void updatesEveryFieldWithoutTransposingParameters() {
        Food existing = new Food("기존", 1, 1.0, 2.0, 3.0, null, false);
        when(foodRepository.findById(1L)).thenReturn(Optional.of(existing));

        FoodResponse response = foodService.update(1L, new FoodRequest("닭가슴살", 165, 0.0, 3.6, 31.0, "100g", true));

        assertThat(response.name()).isEqualTo("닭가슴살");
        assertThat(response.calories()).isEqualTo(165);
        assertThat(response.carbsG()).isEqualTo(0.0);
        assertThat(response.fatG()).isEqualTo(3.6);
        assertThat(response.proteinG()).isEqualTo(31.0);
        assertThat(response.serving()).isEqualTo("100g");
        assertThat(response.pinned()).isTrue();
    }

    @Test
    void throwsWhenFoodNotFound() {
        when(foodRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> foodService.delete(9L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("9");
    }
}
