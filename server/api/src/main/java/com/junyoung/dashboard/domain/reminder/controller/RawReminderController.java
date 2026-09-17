package com.junyoung.dashboard.domain.reminder.controller;

import com.junyoung.dashboard.domain.reminder.dto.RawReminderRequest;
import com.junyoung.dashboard.domain.reminder.dto.RawReminderResponse;
import com.junyoung.dashboard.domain.reminder.service.RawReminderService;
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
@RequestMapping("/api/reminder/raw/reminders")
public class RawReminderController {

    private final RawReminderService rawReminderService;

    public RawReminderController(RawReminderService rawReminderService) {
        this.rawReminderService = rawReminderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RawReminderResponse> create(@Valid @RequestBody RawReminderRequest request) {
        return ApiResponse.success(rawReminderService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RawReminderResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(rawReminderService.findById(id));
    }
}
