package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.DietGoal;
import com.junyoung.dashboard.domain.health.entity.GoalRule;

import java.time.LocalDateTime;

public record DietGoalResponse(
        Long id,
        Double carbsG,
        GoalRule carbsRule,
        Double fatG,
        GoalRule fatRule,
        Double proteinG,
        GoalRule proteinRule,
        Integer calories,
        GoalRule caloriesRule,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DietGoalResponse from(DietGoal goal) {
        return new DietGoalResponse(
                goal.getId(),
                goal.getCarbsG(), goal.getCarbsRule(),
                goal.getFatG(), goal.getFatRule(),
                goal.getProteinG(), goal.getProteinRule(),
                goal.getCalories(), goal.getCaloriesRule(),
                goal.getCreatedAt(), goal.getUpdatedAt()
        );
    }
}
