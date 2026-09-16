package com.junyoung.dashboard.domain.life.controller;

import com.junyoung.dashboard.domain.life.dto.HabitRequest;
import com.junyoung.dashboard.domain.life.dto.HabitResponse;
import com.junyoung.dashboard.domain.life.service.HabitService;
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
@RequestMapping("/api/life/habits")
public class HabitController {

    private final HabitService habitService;

    public HabitController(HabitService habitService) {
        this.habitService = habitService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HabitResponse> create(@Valid @RequestBody HabitRequest request) {
        return ApiResponse.success(habitService.create(request));
    }

    @GetMapping
    public ApiResponse<List<HabitResponse>> findAll() {
        return ApiResponse.success(habitService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<HabitResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(habitService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<HabitResponse> update(@PathVariable Long id, @Valid @RequestBody HabitRequest request) {
        return ApiResponse.success(habitService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        habitService.delete(id);
    }
}
