package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.HealthLogRequest;
import com.junyoung.dashboard.domain.health.dto.HealthLogResponse;
import com.junyoung.dashboard.domain.health.service.HealthLogService;
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
@RequestMapping("/api/health/logs")
public class HealthLogController {

    private final HealthLogService healthLogService;

    public HealthLogController(HealthLogService healthLogService) {
        this.healthLogService = healthLogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HealthLogResponse> create(@Valid @RequestBody HealthLogRequest request) {
        return ApiResponse.success(healthLogService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<HealthLogResponse>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(healthLogService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<HealthLogResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(healthLogService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<HealthLogResponse> update(@PathVariable Long id, @Valid @RequestBody HealthLogRequest request) {
        return ApiResponse.success(healthLogService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        healthLogService.delete(id);
    }
}
