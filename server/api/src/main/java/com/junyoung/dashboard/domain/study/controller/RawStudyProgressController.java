package com.junyoung.dashboard.domain.study.controller;

import com.junyoung.dashboard.domain.study.dto.RawStudyProgressRequest;
import com.junyoung.dashboard.domain.study.dto.RawStudyProgressResponse;
import com.junyoung.dashboard.domain.study.service.RawStudyProgressService;
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
@RequestMapping("/api/study/raw/progresses")
public class RawStudyProgressController {

    private final RawStudyProgressService rawStudyProgressService;

    public RawStudyProgressController(RawStudyProgressService rawStudyProgressService) {
        this.rawStudyProgressService = rawStudyProgressService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RawStudyProgressResponse> create(@Valid @RequestBody RawStudyProgressRequest request) {
        return ApiResponse.success(rawStudyProgressService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RawStudyProgressResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(rawStudyProgressService.findById(id));
    }
}
