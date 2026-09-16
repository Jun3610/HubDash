package com.junyoung.dashboard.domain.hub.controller;

import com.junyoung.dashboard.domain.hub.dto.HubCategoryRequest;
import com.junyoung.dashboard.domain.hub.dto.HubCategoryResponse;
import com.junyoung.dashboard.domain.hub.service.HubCategoryService;
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
@RequestMapping("/api/hub/categories")
public class HubCategoryController {

    private final HubCategoryService hubCategoryService;

    public HubCategoryController(HubCategoryService hubCategoryService) {
        this.hubCategoryService = hubCategoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HubCategoryResponse> create(@Valid @RequestBody HubCategoryRequest request) {
        return ApiResponse.success(hubCategoryService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<HubCategoryResponse>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(hubCategoryService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<HubCategoryResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(hubCategoryService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<HubCategoryResponse> update(@PathVariable Long id, @Valid @RequestBody HubCategoryRequest request) {
        return ApiResponse.success(hubCategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        hubCategoryService.delete(id);
    }
}
