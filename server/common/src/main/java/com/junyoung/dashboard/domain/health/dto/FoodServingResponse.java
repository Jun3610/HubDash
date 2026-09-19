package com.junyoung.dashboard.domain.health.dto;

// 영양 필드는 MealRecordRequest(calories/carbsG/proteinG/fatG/sodiumMg)와 이름·단위를 맞춰
// 클라이언트가 그대로 식사 기록 생성 요청에 옮겨 쓸 수 있게 한다. FatSecret이 값을 주지 않은 항목은 null.
public record FoodServingResponse(
        String servingId,
        String description,
        Double metricServingAmount,
        String metricServingUnit,
        Integer calories,
        Double carbsG,
        Double proteinG,
        Double fatG,
        Double sodiumMg
) {
}
