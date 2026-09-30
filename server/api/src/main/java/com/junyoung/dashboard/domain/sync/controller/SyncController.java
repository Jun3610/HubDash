package com.junyoung.dashboard.domain.sync.controller;

import com.junyoung.dashboard.domain.sync.dto.SyncResponse;
import com.junyoung.dashboard.domain.sync.dto.SyncStatusResponse;
import com.junyoung.dashboard.domain.sync.service.ScheduleSyncService;
import com.junyoung.dashboard.global.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 웹의 "갱신" 버튼 (이슈 #228) */
@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final ScheduleSyncService service;

    public SyncController(ScheduleSyncService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<SyncResponse> sync() {
        return ApiResponse.success(service.syncAll());
    }

    @GetMapping("/status")
    public ApiResponse<SyncStatusResponse> status() {
        return ApiResponse.success(service.status());
    }
}
