package com.junyoung.dashboard.domain.schedule.controller;

import com.junyoung.dashboard.domain.schedule.dto.RawEventRequest;
import com.junyoung.dashboard.domain.schedule.dto.RawEventResponse;
import com.junyoung.dashboard.domain.schedule.service.RawEventService;
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
@RequestMapping("/api/schedule/raw/events")
public class RawEventController {

    private final RawEventService rawEventService;

    public RawEventController(RawEventService rawEventService) {
        this.rawEventService = rawEventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RawEventResponse> create(@Valid @RequestBody RawEventRequest request) {
        return ApiResponse.success(rawEventService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RawEventResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(rawEventService.findById(id));
    }
}
