package com.junyoung.dashboard.domain.life.controller;

import com.junyoung.dashboard.analytics.life.HabitWeeklyStatJobConfig;
import com.junyoung.dashboard.pipeline.launcher.WeeklyStatJobRunner;
import com.junyoung.dashboard.domain.life.dto.HabitWeeklyStatBatchRunRequest;
import com.junyoung.dashboard.domain.life.dto.HabitWeeklyStatBatchRunResponse;
import com.junyoung.dashboard.domain.life.dto.HabitWeeklyStatResponse;
import com.junyoung.dashboard.domain.life.service.HabitWeeklyStatService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/life/analytics/habit-weekly-stats")
public class HabitWeeklyStatController {

    private final HabitWeeklyStatService habitWeeklyStatService;
    private final WeeklyStatJobRunner jobRunner;

    public HabitWeeklyStatController(HabitWeeklyStatService habitWeeklyStatService,
                                      WeeklyStatJobRunner jobRunner) {
        this.habitWeeklyStatService = habitWeeklyStatService;
        this.jobRunner = jobRunner;
    }

    @GetMapping
    public ApiResponse<PageResponse<HabitWeeklyStatResponse>> findByHabitId(
            @RequestParam Long habitId, @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(habitWeeklyStatService.findByHabitId(habitId, pageable)));
    }

    // 매주 월요일 스케줄 실행을 기다리지 않고 수동으로 즉시 집계를 트리거하기 위한 엔드포인트(검증/재집계 용도).
    @PostMapping("/batch-runs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<HabitWeeklyStatBatchRunResponse> runBatch(
            @RequestBody(required = false) HabitWeeklyStatBatchRunRequest request) throws Exception {
        LocalDate weekStart = request != null ? request.weekStart() : null;
        JobExecution execution = jobRunner.run(HabitWeeklyStatJobConfig.JOB_NAME, weekStart);
        LocalDate resolvedWeekStart = LocalDate.parse(execution.getJobParameters().getString("weekStart"));
        return ApiResponse.success(new HabitWeeklyStatBatchRunResponse(
                execution.getId(), execution.getStatus().toString(), resolvedWeekStart));
    }
}
