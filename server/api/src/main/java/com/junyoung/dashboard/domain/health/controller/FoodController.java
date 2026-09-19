package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.FoodDetailResponse;
import com.junyoung.dashboard.domain.health.dto.FoodSearchResponse;
import com.junyoung.dashboard.domain.health.external.fatsecret.FatSecretClient;
import com.junyoung.dashboard.global.common.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// FatSecret 영양 정보 조회 — 결과의 영양 필드는 MealRecordRequest와 같은 이름/단위라서
// 클라이언트가 선택한 serving을 그대로 /api/health/meal-records 생성 요청에 옮겨 쓴다.
@RestController
@RequestMapping("/api/health/foods")
public class FoodController {

    private final FatSecretClient fatSecretClient;

    public FoodController(FatSecretClient fatSecretClient) {
        this.fatSecretClient = fatSecretClient;
    }

    @GetMapping("/search")
    public ApiResponse<FoodSearchResponse> search(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ApiResponse.success(fatSecretClient.searchFoods(query, page, size));
    }

    @GetMapping("/{foodId}")
    public ApiResponse<FoodDetailResponse> get(@PathVariable String foodId) {
        return ApiResponse.success(fatSecretClient.getFood(foodId));
    }
}
