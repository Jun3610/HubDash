package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.FoodRequest;
import com.junyoung.dashboard.domain.health.dto.FoodResponse;
import com.junyoung.dashboard.domain.health.service.FoodService;
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
@RequestMapping("/api/health/foods")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FoodResponse> create(@Valid @RequestBody FoodRequest request) {
        return ApiResponse.success(foodService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<FoodResponse>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(foodService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<FoodResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(foodService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<FoodResponse> update(@PathVariable Long id, @Valid @RequestBody FoodRequest request) {
        return ApiResponse.success(foodService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        foodService.delete(id);
    }
}
