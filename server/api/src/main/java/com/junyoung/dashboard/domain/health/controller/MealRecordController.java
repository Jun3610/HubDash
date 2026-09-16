package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.MealRecordRequest;
import com.junyoung.dashboard.domain.health.dto.MealRecordResponse;
import com.junyoung.dashboard.domain.health.service.MealRecordService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health/meal-records")
public class MealRecordController {

    private final MealRecordService mealRecordService;

    public MealRecordController(MealRecordService mealRecordService) {
        this.mealRecordService = mealRecordService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MealRecordResponse> create(@Valid @RequestBody MealRecordRequest request) {
        return ApiResponse.success(mealRecordService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<MealRecordResponse>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(mealRecordService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<MealRecordResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(mealRecordService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<MealRecordResponse> update(@PathVariable Long id, @Valid @RequestBody MealRecordRequest request) {
        return ApiResponse.success(mealRecordService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        mealRecordService.delete(id);
    }
}
