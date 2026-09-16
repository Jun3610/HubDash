package com.junyoung.dashboard.domain.life.controller;

import com.junyoung.dashboard.domain.life.dto.ReadingLogRequest;
import com.junyoung.dashboard.domain.life.dto.ReadingLogResponse;
import com.junyoung.dashboard.domain.life.service.ReadingLogService;
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
@RequestMapping("/api/life/reading-logs")
public class ReadingLogController {

    private final ReadingLogService readingLogService;

    public ReadingLogController(ReadingLogService readingLogService) {
        this.readingLogService = readingLogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReadingLogResponse> create(@Valid @RequestBody ReadingLogRequest request) {
        return ApiResponse.success(readingLogService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<ReadingLogResponse>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(readingLogService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReadingLogResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(readingLogService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ReadingLogResponse> update(@PathVariable Long id, @Valid @RequestBody ReadingLogRequest request) {
        return ApiResponse.success(readingLogService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        readingLogService.delete(id);
    }
}
