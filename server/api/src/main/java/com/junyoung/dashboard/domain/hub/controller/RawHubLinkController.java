package com.junyoung.dashboard.domain.hub.controller;

import com.junyoung.dashboard.domain.hub.dto.RawHubLinkRequest;
import com.junyoung.dashboard.domain.hub.dto.RawHubLinkResponse;
import com.junyoung.dashboard.domain.hub.service.RawHubLinkService;
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
@RequestMapping("/api/hub/raw/links")
public class RawHubLinkController {

    private final RawHubLinkService rawHubLinkService;

    public RawHubLinkController(RawHubLinkService rawHubLinkService) {
        this.rawHubLinkService = rawHubLinkService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RawHubLinkResponse> create(@Valid @RequestBody RawHubLinkRequest request) {
        return ApiResponse.success(rawHubLinkService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RawHubLinkResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(rawHubLinkService.findById(id));
    }
}
