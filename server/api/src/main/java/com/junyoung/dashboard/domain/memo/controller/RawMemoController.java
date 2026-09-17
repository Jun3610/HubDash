package com.junyoung.dashboard.domain.memo.controller;

import com.junyoung.dashboard.domain.memo.dto.RawMemoRequest;
import com.junyoung.dashboard.domain.memo.dto.RawMemoResponse;
import com.junyoung.dashboard.domain.memo.service.RawMemoService;
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
@RequestMapping("/api/memo/raw/memos")
public class RawMemoController {

    private final RawMemoService rawMemoService;

    public RawMemoController(RawMemoService rawMemoService) {
        this.rawMemoService = rawMemoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RawMemoResponse> create(@Valid @RequestBody RawMemoRequest request) {
        return ApiResponse.success(rawMemoService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RawMemoResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(rawMemoService.findById(id));
    }
}
