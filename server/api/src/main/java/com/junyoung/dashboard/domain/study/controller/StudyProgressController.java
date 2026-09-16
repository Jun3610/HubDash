package com.junyoung.dashboard.domain.study.controller;

import com.junyoung.dashboard.domain.study.dto.StudyProgressRequest;
import com.junyoung.dashboard.domain.study.dto.StudyProgressResponse;
import com.junyoung.dashboard.domain.study.service.StudyProgressService;
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
@RequestMapping("/api/study/progresses")
public class StudyProgressController {

    private final StudyProgressService studyProgressService;

    public StudyProgressController(StudyProgressService studyProgressService) {
        this.studyProgressService = studyProgressService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudyProgressResponse> create(@Valid @RequestBody StudyProgressRequest request) {
        return ApiResponse.success(studyProgressService.create(request));
    }

    @GetMapping
    public ApiResponse<List<StudyProgressResponse>> findByTopicId(@RequestParam Long topicId) {
        return ApiResponse.success(studyProgressService.findByTopicId(topicId));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudyProgressResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(studyProgressService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<StudyProgressResponse> update(@PathVariable Long id, @Valid @RequestBody StudyProgressRequest request) {
        return ApiResponse.success(studyProgressService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        studyProgressService.delete(id);
    }
}
