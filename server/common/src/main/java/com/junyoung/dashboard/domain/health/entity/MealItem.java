package com.junyoung.dashboard.domain.health.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 끼니(MealRecord)에 추가한 음식 한 개. 칼로리는 라벨 값을 그대로 입력받고, 탄단지/나트륨은 모르면 비워둘 수 있다.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "health_meal_item")
public class MealItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_record_id", nullable = false)
    private MealRecord mealRecord;

    @Column(nullable = false, length = 100)
    private String name;

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

    public MealItem(MealRecord mealRecord, String name, Integer calories,
                     Double carbsG, Double proteinG, Double fatG, Double sodiumMg) {
        this.mealRecord = mealRecord;
        this.name = name;
        this.calories = calories;
        this.carbsG = carbsG;
        this.proteinG = proteinG;
        this.fatG = fatG;
        this.sodiumMg = sodiumMg;
    }

    public void update(MealRecord mealRecord, String name, Integer calories,
                        Double carbsG, Double proteinG, Double fatG, Double sodiumMg) {
        this.mealRecord = mealRecord;
        this.name = name;
        this.calories = calories;
        this.carbsG = carbsG;
        this.proteinG = proteinG;
        this.fatG = fatG;
        this.sodiumMg = sodiumMg;
    }
}
