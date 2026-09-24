package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.DietGoalRequest;
import com.junyoung.dashboard.domain.health.dto.DietGoalResponse;
import com.junyoung.dashboard.domain.health.entity.DietGoal;
import com.junyoung.dashboard.domain.health.entity.GoalRule;
import com.junyoung.dashboard.domain.health.repository.DietGoalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DietGoalServiceTest {

    @Mock
    private DietGoalRepository dietGoalRepository;

    private DietGoalService dietGoalService;

    @BeforeEach
    void setUp() {
        dietGoalService = new DietGoalService(dietGoalRepository);
    }

    @Test
    void firstReadCreatesEmptyGoalWithoutAnyPresetValues() {
        when(dietGoalRepository.findAll()).thenReturn(List.of());
        when(dietGoalRepository.save(any(DietGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        DietGoalResponse response = dietGoalService.get();

        // 사용자가 정하기 전에는 어떤 목표값도 없어야 한다 (예전 하드코딩 기본값 문제, 이슈 #131)
        assertThat(response.carbsG()).isNull();
        assertThat(response.fatG()).isNull();
        assertThat(response.proteinG()).isNull();
        assertThat(response.calories()).isNull();
        assertThat(response.carbsRule()).isEqualTo(GoalRule.AT_MOST);
        assertThat(response.proteinRule()).isEqualTo(GoalRule.AT_LEAST);
    }

    @Test
    void updatesExistingRowInsteadOfCreatingAnother() {
        DietGoal existing = DietGoal.empty();
        when(dietGoalRepository.findAll()).thenReturn(List.of(existing));

        DietGoalResponse response = dietGoalService.update(new DietGoalRequest(
                140.0, GoalRule.AT_MOST, 50.0, GoalRule.AT_MOST, 160.0, GoalRule.AT_LEAST, 1500, GoalRule.AT_LEAST));

        verify(dietGoalRepository, never()).save(any());
        assertThat(response.carbsG()).isEqualTo(140.0);
        assertThat(response.fatG()).isEqualTo(50.0);
        assertThat(response.proteinG()).isEqualTo(160.0);
        assertThat(response.calories()).isEqualTo(1500);
        assertThat(response.caloriesRule()).isEqualTo(GoalRule.AT_LEAST);
    }
}
