package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.MealItemRequest;
import com.junyoung.dashboard.domain.health.dto.MealItemResponse;
import com.junyoung.dashboard.domain.health.service.MealItemService;
import com.junyoung.dashboard.global.common.ApiResponse;
import com.junyoung.dashboard.global.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health/meal-items")
public class MealItemController {

    private final MealItemService mealItemService;

    public MealItemController(MealItemService mealItemService) {
        this.mealItemService = mealItemService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MealItemResponse> create(@Valid @RequestBody MealItemRequest request) {
        return ApiResponse.success(mealItemService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<MealItemResponse>> findByMealRecordId(
            @RequestParam Long mealRecordId, @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(mealItemService.findByMealRecordId(mealRecordId, pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<MealItemResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(mealItemService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<MealItemResponse> update(@PathVariable Long id, @Valid @RequestBody MealItemRequest request) {
        return ApiResponse.success(mealItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        mealItemService.delete(id);
    }
}
