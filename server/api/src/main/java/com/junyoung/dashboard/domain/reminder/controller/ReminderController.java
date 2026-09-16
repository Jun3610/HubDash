package com.junyoung.dashboard.domain.reminder.controller;

import com.junyoung.dashboard.domain.reminder.dto.ReminderRequest;
import com.junyoung.dashboard.domain.reminder.dto.ReminderResponse;
import com.junyoung.dashboard.domain.reminder.service.ReminderService;
import com.junyoung.dashboard.global.common.ApiResponse;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/reminder/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReminderResponse> create(@Valid @RequestBody ReminderRequest request) {
        return ApiResponse.success(reminderService.create(request));
    }

    @GetMapping
    public ApiResponse<List<ReminderResponse>> findAll() {
        return ApiResponse.success(reminderService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<ReminderResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(reminderService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ReminderResponse> update(@PathVariable Long id, @Valid @RequestBody ReminderRequest request) {
        return ApiResponse.success(reminderService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        reminderService.delete(id);
    }
}
