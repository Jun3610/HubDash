package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.MealItem;

import java.util.Collection;

// 음식 항목들의 영양 합계. 값을 비워둔 항목(탄단지 미입력 등)은 0으로 취급한다.
public record MealTotals(Integer calories, Double carbsG, Double proteinG, Double fatG, Double sodiumMg) {

    public static MealTotals of(Collection<MealItem> items) {
        int calories = 0;
        double carbs = 0;
        double protein = 0;
        double fat = 0;
        double sodium = 0;
        for (MealItem item : items) {
            calories += item.getCalories();
            carbs += orZero(item.getCarbsG());
            protein += orZero(item.getProteinG());
            fat += orZero(item.getFatG());
            sodium += orZero(item.getSodiumMg());
        }
        return new MealTotals(calories, round(carbs), round(protein), round(fat), round(sodium));
    }

    private static double orZero(Double value) {
        return value == null ? 0 : value;
    }

    // 0.1 + 0.2 같은 부동소수점 누적 오차가 응답에 노출되지 않도록 소수 둘째 자리로 맞춘다.
    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
