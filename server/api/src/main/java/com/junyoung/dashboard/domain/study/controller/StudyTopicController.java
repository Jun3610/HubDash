package com.junyoung.dashboard.domain.study.controller;

import com.junyoung.dashboard.domain.study.dto.StudyTopicRequest;
import com.junyoung.dashboard.domain.study.dto.StudyTopicResponse;
import com.junyoung.dashboard.domain.study.service.StudyTopicService;
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
@RequestMapping("/api/study/topics")
public class StudyTopicController {

    private final StudyTopicService studyTopicService;

    public StudyTopicController(StudyTopicService studyTopicService) {
        this.studyTopicService = studyTopicService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudyTopicResponse> create(@Valid @RequestBody StudyTopicRequest request) {
        return ApiResponse.success(studyTopicService.create(request));
    }

    @GetMapping
    public ApiResponse<List<StudyTopicResponse>> findAll() {
        return ApiResponse.success(studyTopicService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<StudyTopicResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(studyTopicService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<StudyTopicResponse> update(@PathVariable Long id, @Valid @RequestBody StudyTopicRequest request) {
        return ApiResponse.success(studyTopicService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        studyTopicService.delete(id);
    }
}
