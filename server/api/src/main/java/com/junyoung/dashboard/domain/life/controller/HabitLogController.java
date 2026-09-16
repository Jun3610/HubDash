package com.junyoung.dashboard.domain.life.controller;

import com.junyoung.dashboard.domain.life.dto.HabitLogRequest;
import com.junyoung.dashboard.domain.life.dto.HabitLogResponse;
import com.junyoung.dashboard.domain.life.service.HabitLogService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/life/habit-logs")
public class HabitLogController {

    private final HabitLogService habitLogService;

    public HabitLogController(HabitLogService habitLogService) {
        this.habitLogService = habitLogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HabitLogResponse> create(@Valid @RequestBody HabitLogRequest request) {
        return ApiResponse.success(habitLogService.create(request));
    }

    @GetMapping
    public ApiResponse<List<HabitLogResponse>> findByHabitId(@RequestParam Long habitId) {
        return ApiResponse.success(habitLogService.findByHabitId(habitId));
    }

    @GetMapping("/{id}")
    public ApiResponse<HabitLogResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(habitLogService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<HabitLogResponse> update(@PathVariable Long id, @Valid @RequestBody HabitLogRequest request) {
        return ApiResponse.success(habitLogService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        habitLogService.delete(id);
    }
}
