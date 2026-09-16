package com.junyoung.dashboard.domain.memo.controller;

import com.junyoung.dashboard.domain.memo.dto.MemoRequest;
import com.junyoung.dashboard.domain.memo.dto.MemoResponse;
import com.junyoung.dashboard.domain.memo.service.MemoService;
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
@RequestMapping("/api/memo/memos")
public class MemoController {

    private final MemoService memoService;

    public MemoController(MemoService memoService) {
        this.memoService = memoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemoResponse> create(@Valid @RequestBody MemoRequest request) {
        return ApiResponse.success(memoService.create(request));
    }

    @GetMapping
    public ApiResponse<List<MemoResponse>> findAll() {
        return ApiResponse.success(memoService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<MemoResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(memoService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<MemoResponse> update(@PathVariable Long id, @Valid @RequestBody MemoRequest request) {
        return ApiResponse.success(memoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        memoService.delete(id);
    }
}
