package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.MealType;

import java.time.LocalDate;
import java.util.List;

// 하루 총합 + 끼니 종류별 합계. 기록이 없는 끼니도 0으로 채워 항상 4개 끼니를 돌려준다.
public record DailyMealSummaryResponse(LocalDate date, MealTotals totals, List<MealTypeSummary> meals) {

    public record MealTypeSummary(MealType mealType, int itemCount, MealTotals totals) {
    }
}
