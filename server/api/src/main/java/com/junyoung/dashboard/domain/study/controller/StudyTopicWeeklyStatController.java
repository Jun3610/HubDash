package com.junyoung.dashboard.domain.study.controller;

import com.junyoung.dashboard.analytics.study.StudyTopicWeeklyStatBatchService;
import com.junyoung.dashboard.domain.study.dto.StudyTopicWeeklyStatBatchRunRequest;
import com.junyoung.dashboard.domain.study.dto.StudyTopicWeeklyStatBatchRunResponse;
import com.junyoung.dashboard.domain.study.dto.StudyTopicWeeklyStatResponse;
import com.junyoung.dashboard.domain.study.service.StudyTopicWeeklyStatService;
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
@RequestMapping("/api/study/analytics/topic-weekly-stats")
public class StudyTopicWeeklyStatController {

    private final StudyTopicWeeklyStatService studyTopicWeeklyStatService;
    private final StudyTopicWeeklyStatBatchService batchService;

    public StudyTopicWeeklyStatController(StudyTopicWeeklyStatService studyTopicWeeklyStatService,
                                           StudyTopicWeeklyStatBatchService batchService) {
        this.studyTopicWeeklyStatService = studyTopicWeeklyStatService;
        this.batchService = batchService;
    }

    @GetMapping
    public ApiResponse<PageResponse<StudyTopicWeeklyStatResponse>> findByTopicId(
            @RequestParam Long topicId, @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(studyTopicWeeklyStatService.findByTopicId(topicId, pageable)));
    }

    @PostMapping("/batch-runs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<StudyTopicWeeklyStatBatchRunResponse> runBatch(
            @RequestBody(required = false) StudyTopicWeeklyStatBatchRunRequest request) throws Exception {
        LocalDate weekStart = request != null ? request.weekStart() : null;
        JobExecution execution = batchService.run(weekStart);
        LocalDate resolvedWeekStart = LocalDate.parse(execution.getJobParameters().getString("weekStart"));
        return ApiResponse.success(new StudyTopicWeeklyStatBatchRunResponse(
                execution.getId(), execution.getStatus().toString(), resolvedWeekStart));
    }
}
