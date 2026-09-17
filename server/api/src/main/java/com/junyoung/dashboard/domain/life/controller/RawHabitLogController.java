package com.junyoung.dashboard.domain.life.controller;

import com.junyoung.dashboard.domain.life.dto.RawHabitLogRequest;
import com.junyoung.dashboard.domain.life.dto.RawHabitLogResponse;
import com.junyoung.dashboard.domain.life.service.RawHabitLogService;
import com.junyoung.dashboard.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/life/raw/habit-logs")
public class RawHabitLogController {

    private final RawHabitLogService rawHabitLogService;

    public RawHabitLogController(RawHabitLogService rawHabitLogService) {
        this.rawHabitLogService = rawHabitLogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RawHabitLogResponse> create(@Valid @RequestBody RawHabitLogRequest request) {
        return ApiResponse.success(rawHabitLogService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RawHabitLogResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(rawHabitLogService.findById(id));
    }
}
