package com.junyoung.dashboard.domain.pknu.controller;

import com.junyoung.dashboard.domain.pknu.dto.RawAssignmentRequest;
import com.junyoung.dashboard.domain.pknu.dto.RawAssignmentResponse;
import com.junyoung.dashboard.domain.pknu.service.RawAssignmentService;
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
@RequestMapping("/api/pknu/raw/assignments")
public class RawAssignmentController {

    private final RawAssignmentService rawAssignmentService;

    public RawAssignmentController(RawAssignmentService rawAssignmentService) {
        this.rawAssignmentService = rawAssignmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RawAssignmentResponse> create(@Valid @RequestBody RawAssignmentRequest request) {
        return ApiResponse.success(rawAssignmentService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RawAssignmentResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(rawAssignmentService.findById(id));
    }
}
