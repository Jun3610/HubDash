package com.junyoung.dashboard.domain.health.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 냉장고에 넣어 둔 음식. 끼니에 추가할 때 이 값을 복사해 음식 항목(MealItem)을 만든다. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "health_food")
public class Food extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer calories;

    @Column(name = "carbs_g")
    private Double carbsG;

    @Column(name = "fat_g")
    private Double fatG;

    @Column(name = "protein_g")
    private Double proteinG;

    /** 1회량 메모 (예: "1개", "200g") */
    @Column(length = 50)
    private String serving;

    /** 자주 먹는 음식으로 고정 — 목록 맨 앞에 보인다 */
    @Column(nullable = false)
    private boolean pinned;

    public Food(String name, Integer calories, Double carbsG, Double fatG, Double proteinG, String serving, boolean pinned) {
        this.name = name;
        this.calories = calories;
        this.carbsG = carbsG;
        this.fatG = fatG;
        this.proteinG = proteinG;
        this.serving = serving;
        this.pinned = pinned;
    }

    public void update(String name, Integer calories, Double carbsG, Double fatG, Double proteinG, String serving, boolean pinned) {
        this.name = name;
        this.calories = calories;
        this.carbsG = carbsG;
        this.fatG = fatG;
        this.proteinG = proteinG;
        this.serving = serving;
        this.pinned = pinned;
    }
}
