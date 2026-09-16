package com.junyoung.dashboard.domain.pknu.controller;

import com.junyoung.dashboard.domain.pknu.dto.CourseRequest;
import com.junyoung.dashboard.domain.pknu.dto.CourseResponse;
import com.junyoung.dashboard.domain.pknu.service.CourseService;
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
@RequestMapping("/api/pknu/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CourseResponse> create(@Valid @RequestBody CourseRequest request) {
        return ApiResponse.success(courseService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<CourseResponse>> findBySemesterId(
            @RequestParam Long semesterId, @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(courseService.findBySemesterId(semesterId, pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(courseService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<CourseResponse> update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return ApiResponse.success(courseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courseService.delete(id);
    }
}
