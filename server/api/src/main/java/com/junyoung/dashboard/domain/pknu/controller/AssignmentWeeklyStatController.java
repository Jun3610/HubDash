package com.junyoung.dashboard.domain.pknu.controller;

import com.junyoung.dashboard.analytics.pknu.AssignmentWeeklyStatJobConfig;
import com.junyoung.dashboard.pipeline.launcher.WeeklyStatJobRunner;
import com.junyoung.dashboard.domain.pknu.dto.AssignmentWeeklyStatBatchRunRequest;
import com.junyoung.dashboard.domain.pknu.dto.AssignmentWeeklyStatBatchRunResponse;
import com.junyoung.dashboard.domain.pknu.dto.AssignmentWeeklyStatResponse;
import com.junyoung.dashboard.domain.pknu.service.AssignmentWeeklyStatService;
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
@RequestMapping("/api/pknu/analytics/assignment-weekly-stats")
public class AssignmentWeeklyStatController {

    private final AssignmentWeeklyStatService assignmentWeeklyStatService;
    private final WeeklyStatJobRunner jobRunner;

    public AssignmentWeeklyStatController(AssignmentWeeklyStatService assignmentWeeklyStatService,
                                           WeeklyStatJobRunner jobRunner) {
        this.assignmentWeeklyStatService = assignmentWeeklyStatService;
        this.jobRunner = jobRunner;
    }

    @GetMapping
    public ApiResponse<PageResponse<AssignmentWeeklyStatResponse>> findByCourseId(
            @RequestParam Long courseId, @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(assignmentWeeklyStatService.findByCourseId(courseId, pageable)));
    }

    @PostMapping("/batch-runs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<AssignmentWeeklyStatBatchRunResponse> runBatch(
            @RequestBody(required = false) AssignmentWeeklyStatBatchRunRequest request) throws Exception {
        LocalDate weekStart = request != null ? request.weekStart() : null;
        JobExecution execution = jobRunner.run(AssignmentWeeklyStatJobConfig.JOB_NAME, weekStart);
        LocalDate resolvedWeekStart = LocalDate.parse(execution.getJobParameters().getString("weekStart"));
        return ApiResponse.success(new AssignmentWeeklyStatBatchRunResponse(
                execution.getId(), execution.getStatus().toString(), resolvedWeekStart));
    }
}
