package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.DietGoalRequest;
import com.junyoung.dashboard.domain.health.dto.DietGoalResponse;
import com.junyoung.dashboard.domain.health.service.DietGoalService;
import com.junyoung.dashboard.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health/diet-goal")
public class DietGoalController {

    private final DietGoalService dietGoalService;

    public DietGoalController(DietGoalService dietGoalService) {
        this.dietGoalService = dietGoalService;
    }

    @GetMapping
    public ApiResponse<DietGoalResponse> get() {
        return ApiResponse.success(dietGoalService.get());
    }

    @PutMapping
    public ApiResponse<DietGoalResponse> update(@Valid @RequestBody DietGoalRequest request) {
        return ApiResponse.success(dietGoalService.update(request));
    }
}
