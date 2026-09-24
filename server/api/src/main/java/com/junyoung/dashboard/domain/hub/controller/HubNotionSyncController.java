package com.junyoung.dashboard.domain.hub.controller;

import com.junyoung.dashboard.domain.hub.service.HubNotionSyncService;
import com.junyoung.dashboard.global.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 웹의 'Notion 불러오기' 버튼 (이슈 #163) */
@RestController
public class HubNotionSyncController {

    private final HubNotionSyncService service;

    public HubNotionSyncController(HubNotionSyncService service) {
        this.service = service;
    }

    @PostMapping("/api/hub/notion-sync")
    public ApiResponse<HubNotionSyncService.SyncResult> sync() {
        return ApiResponse.success(service.sync());
    }
}
