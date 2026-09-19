package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.analytics.health.MealWeeklyStatBatchService;
import com.junyoung.dashboard.domain.health.dto.MealWeeklyStatBatchRunRequest;
import com.junyoung.dashboard.domain.health.dto.MealWeeklyStatBatchRunResponse;
import com.junyoung.dashboard.domain.health.dto.MealWeeklyStatResponse;
import com.junyoung.dashboard.domain.health.service.MealWeeklyStatService;
import com.junyoung.dashboard.global.common.ApiResponse;
import com.junyoung.dashboard.global.common.PageResponse;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/health/analytics/meal-weekly-stats")
public class MealWeeklyStatController {

    private final MealWeeklyStatService mealWeeklyStatService;
    private final MealWeeklyStatBatchService batchService;

    public MealWeeklyStatController(MealWeeklyStatService mealWeeklyStatService,
                                          MealWeeklyStatBatchService batchService) {
        this.mealWeeklyStatService = mealWeeklyStatService;
        this.batchService = batchService;
    }

    @GetMapping
    public ApiResponse<PageResponse<MealWeeklyStatResponse>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(mealWeeklyStatService.findAll(pageable)));
    }

    @PostMapping("/batch-runs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<MealWeeklyStatBatchRunResponse> runBatch(
            @RequestBody(required = false) MealWeeklyStatBatchRunRequest request) throws Exception {
        LocalDate weekStart = request != null ? request.weekStart() : null;
        JobExecution execution = batchService.run(weekStart);
        LocalDate resolvedWeekStart = LocalDate.parse(execution.getJobParameters().getString("weekStart"));
        return ApiResponse.success(new MealWeeklyStatBatchRunResponse(
                execution.getId(), execution.getStatus().toString(), resolvedWeekStart));
    }
}
