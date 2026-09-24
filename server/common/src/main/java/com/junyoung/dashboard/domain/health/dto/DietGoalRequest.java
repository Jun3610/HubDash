package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.GoalRule;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** 값은 비워 두면(null) 목표 없음. 방향(이상/이하)은 항상 필요 */
public record DietGoalRequest(
        @PositiveOrZero Double carbsG,
        @NotNull GoalRule carbsRule,
        @PositiveOrZero Double fatG,
        @NotNull GoalRule fatRule,
        @PositiveOrZero Double proteinG,
        @NotNull GoalRule proteinRule,
        @PositiveOrZero Integer calories,
        @NotNull GoalRule caloriesRule
) {
}
