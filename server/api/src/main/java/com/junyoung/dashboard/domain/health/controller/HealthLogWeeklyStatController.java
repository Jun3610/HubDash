package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.analytics.health.HealthLogWeeklyStatBatchService;
import com.junyoung.dashboard.domain.health.dto.HealthLogWeeklyStatBatchRunRequest;
import com.junyoung.dashboard.domain.health.dto.HealthLogWeeklyStatBatchRunResponse;
import com.junyoung.dashboard.domain.health.dto.HealthLogWeeklyStatResponse;
import com.junyoung.dashboard.domain.health.service.HealthLogWeeklyStatService;
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
@RequestMapping("/api/health/analytics/weekly-stats")
public class HealthLogWeeklyStatController {

    private final HealthLogWeeklyStatService healthLogWeeklyStatService;
    private final HealthLogWeeklyStatBatchService batchService;

    public HealthLogWeeklyStatController(HealthLogWeeklyStatService healthLogWeeklyStatService,
                                          HealthLogWeeklyStatBatchService batchService) {
        this.healthLogWeeklyStatService = healthLogWeeklyStatService;
        this.batchService = batchService;
    }

    @GetMapping
    public ApiResponse<PageResponse<HealthLogWeeklyStatResponse>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(healthLogWeeklyStatService.findAll(pageable)));
    }

    @PostMapping("/batch-runs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<HealthLogWeeklyStatBatchRunResponse> runBatch(
            @RequestBody(required = false) HealthLogWeeklyStatBatchRunRequest request) throws Exception {
        LocalDate weekStart = request != null ? request.weekStart() : null;
        JobExecution execution = batchService.run(weekStart);
        LocalDate resolvedWeekStart = LocalDate.parse(execution.getJobParameters().getString("weekStart"));
        return ApiResponse.success(new HealthLogWeeklyStatBatchRunResponse(
                execution.getId(), execution.getStatus().toString(), resolvedWeekStart));
    }
}
