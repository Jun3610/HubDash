package com.junyoung.dashboard.domain.health.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 식단 목표. 사용자 설정처럼 한 행만 쓴다. 값이 null이면 그 항목은 목표를 정하지 않은 상태 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "health_diet_goal")
public class DietGoal extends BaseEntity {

    @Column(name = "carbs_g")
    private Double carbsG;

    @Enumerated(EnumType.STRING)
    @Column(name = "carbs_rule", nullable = false, length = 10)
    private GoalRule carbsRule;

    @Column(name = "fat_g")
    private Double fatG;

    @Enumerated(EnumType.STRING)
    @Column(name = "fat_rule", nullable = false, length = 10)
    private GoalRule fatRule;

    @Column(name = "protein_g")
    private Double proteinG;

    @Enumerated(EnumType.STRING)
    @Column(name = "protein_rule", nullable = false, length = 10)
    private GoalRule proteinRule;

    private Integer calories;

    @Enumerated(EnumType.STRING)
    @Column(name = "calories_rule", nullable = false, length = 10)
    private GoalRule caloriesRule;

    /** 아직 아무 목표도 정하지 않은 상태: 값은 비우고, 흔한 방향(탄수·지방·칼로리 이하, 단백질 이상)만 기본으로 둔다 */
    public static DietGoal empty() {
        DietGoal goal = new DietGoal();
        goal.carbsRule = GoalRule.AT_MOST;
        goal.fatRule = GoalRule.AT_MOST;
        goal.proteinRule = GoalRule.AT_LEAST;
        goal.caloriesRule = GoalRule.AT_MOST;
        return goal;
    }

    public void update(Double carbsG, GoalRule carbsRule, Double fatG, GoalRule fatRule,
                       Double proteinG, GoalRule proteinRule, Integer calories, GoalRule caloriesRule) {
        this.carbsG = carbsG;
        this.carbsRule = carbsRule;
        this.fatG = fatG;
        this.fatRule = fatRule;
        this.proteinG = proteinG;
        this.proteinRule = proteinRule;
        this.calories = calories;
        this.caloriesRule = caloriesRule;
    }
}
