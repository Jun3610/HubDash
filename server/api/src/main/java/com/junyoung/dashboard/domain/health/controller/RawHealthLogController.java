package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.RawHealthLogRequest;
import com.junyoung.dashboard.domain.health.dto.RawHealthLogResponse;
import com.junyoung.dashboard.domain.health.service.RawHealthLogService;
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
@RequestMapping("/api/health/raw/health-logs")
public class RawHealthLogController {

    private final RawHealthLogService rawHealthLogService;

    public RawHealthLogController(RawHealthLogService rawHealthLogService) {
        this.rawHealthLogService = rawHealthLogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RawHealthLogResponse> create(@Valid @RequestBody RawHealthLogRequest request) {
        return ApiResponse.success(rawHealthLogService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RawHealthLogResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(rawHealthLogService.findById(id));
    }
}
