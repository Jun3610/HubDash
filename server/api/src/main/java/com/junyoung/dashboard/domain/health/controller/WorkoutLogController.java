package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.WorkoutLogRequest;
import com.junyoung.dashboard.domain.health.dto.WorkoutLogResponse;
import com.junyoung.dashboard.domain.health.service.WorkoutLogService;
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
@RequestMapping("/api/health/workout-logs")
public class WorkoutLogController {

    private final WorkoutLogService workoutLogService;

    public WorkoutLogController(WorkoutLogService workoutLogService) {
        this.workoutLogService = workoutLogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WorkoutLogResponse> create(@Valid @RequestBody WorkoutLogRequest request) {
        return ApiResponse.success(workoutLogService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<WorkoutLogResponse>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(workoutLogService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkoutLogResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(workoutLogService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<WorkoutLogResponse> update(@PathVariable Long id, @Valid @RequestBody WorkoutLogRequest request) {
        return ApiResponse.success(workoutLogService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        workoutLogService.delete(id);
    }
}
