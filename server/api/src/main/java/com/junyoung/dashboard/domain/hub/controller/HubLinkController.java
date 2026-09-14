package com.junyoung.dashboard.domain.hub.controller;

import com.junyoung.dashboard.domain.hub.dto.HubLinkRequest;
import com.junyoung.dashboard.domain.hub.dto.HubLinkResponse;
import com.junyoung.dashboard.domain.hub.service.HubLinkService;
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
@RequestMapping("/api/hub/links")
public class HubLinkController {

    private final HubLinkService hubLinkService;

    public HubLinkController(HubLinkService hubLinkService) {
        this.hubLinkService = hubLinkService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HubLinkResponse> create(@Valid @RequestBody HubLinkRequest request) {
        return ApiResponse.success(hubLinkService.create(request));
    }

    @GetMapping
    public ApiResponse<List<HubLinkResponse>> findByCategoryId(@RequestParam Long categoryId) {
        return ApiResponse.success(hubLinkService.findByCategoryId(categoryId));
    }

    @GetMapping("/{id}")
    public ApiResponse<HubLinkResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(hubLinkService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<HubLinkResponse> update(@PathVariable Long id, @Valid @RequestBody HubLinkRequest request) {
        return ApiResponse.success(hubLinkService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        hubLinkService.delete(id);
    }
}
