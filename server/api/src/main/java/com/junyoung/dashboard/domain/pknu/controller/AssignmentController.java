package com.junyoung.dashboard.domain.pknu.controller;

import com.junyoung.dashboard.domain.pknu.dto.AssignmentRequest;
import com.junyoung.dashboard.domain.pknu.dto.AssignmentResponse;
import com.junyoung.dashboard.domain.pknu.service.AssignmentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pknu/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AssignmentResponse> create(@Valid @RequestBody AssignmentRequest request) {
        return ApiResponse.success(assignmentService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<AssignmentResponse>> findByCourseId(
            @RequestParam Long courseId, @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(assignmentService.findByCourseId(courseId, pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<AssignmentResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(assignmentService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<AssignmentResponse> update(@PathVariable Long id, @Valid @RequestBody AssignmentRequest request) {
        return ApiResponse.success(assignmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        assignmentService.delete(id);
    }
}
