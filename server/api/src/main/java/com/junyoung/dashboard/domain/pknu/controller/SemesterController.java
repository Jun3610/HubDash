package com.junyoung.dashboard.domain.pknu.controller;

import com.junyoung.dashboard.domain.pknu.dto.SemesterRequest;
import com.junyoung.dashboard.domain.pknu.dto.SemesterResponse;
import com.junyoung.dashboard.domain.pknu.service.SemesterService;
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
@RequestMapping("/api/pknu/semesters")
public class SemesterController {

    private final SemesterService semesterService;

    public SemesterController(SemesterService semesterService) {
        this.semesterService = semesterService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SemesterResponse> create(@Valid @RequestBody SemesterRequest request) {
        return ApiResponse.success(semesterService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<SemesterResponse>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(semesterService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<SemesterResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(semesterService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<SemesterResponse> update(@PathVariable Long id, @Valid @RequestBody SemesterRequest request) {
        return ApiResponse.success(semesterService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        semesterService.delete(id);
    }
}
