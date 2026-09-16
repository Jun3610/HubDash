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

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "health_meal_record")
public class MealRecord extends BaseEntity {

    @Column(name = "consumed_at", nullable = false)
    private LocalDateTime consumedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    @Column(nullable = false)
    private Integer calories;

    @Column(name = "carbs_g")
    private Double carbsG;

    @Column(name = "protein_g")
    private Double proteinG;

    @Column(name = "fat_g")
    private Double fatG;

    @Column(name = "sodium_mg")
    private Double sodiumMg;

    @Column(length = 500)
    private String notes;

    public MealRecord(LocalDateTime consumedAt, MealType mealType, Integer calories,
                       Double carbsG, Double proteinG, Double fatG, Double sodiumMg, String notes) {
        this.consumedAt = consumedAt;
        this.mealType = mealType;
        this.calories = calories;
        this.carbsG = carbsG;
        this.proteinG = proteinG;
        this.fatG = fatG;
        this.sodiumMg = sodiumMg;
        this.notes = notes;
    }

    public void update(LocalDateTime consumedAt, MealType mealType, Integer calories,
                        Double carbsG, Double proteinG, Double fatG, Double sodiumMg, String notes) {
        this.consumedAt = consumedAt;
        this.mealType = mealType;
        this.calories = calories;
        this.carbsG = carbsG;
        this.proteinG = proteinG;
        this.fatG = fatG;
        this.sodiumMg = sodiumMg;
        this.notes = notes;
    }
}
