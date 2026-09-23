package com.junyoung.dashboard.domain.notion.controller;

import com.junyoung.dashboard.domain.notion.dto.NotionSyncResult;
import com.junyoung.dashboard.domain.notion.dto.NotionSyncSourceRequest;
import com.junyoung.dashboard.domain.notion.dto.NotionSyncSourceResponse;
import com.junyoung.dashboard.domain.notion.service.NotionSyncService;
import com.junyoung.dashboard.domain.notion.service.NotionSyncSourceService;
import com.junyoung.dashboard.global.common.ApiResponse;
import com.junyoung.dashboard.global.exception.InvalidRequestException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notion")
public class NotionSyncController {

    private final NotionSyncSourceService sourceService;
    private final NotionSyncService syncService;

    public NotionSyncController(NotionSyncSourceService sourceService, NotionSyncService syncService) {
        this.sourceService = sourceService;
        this.syncService = syncService;
    }

    @PostMapping("/sources")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NotionSyncSourceResponse> createSource(@Valid @RequestBody NotionSyncSourceRequest request) {
        return ApiResponse.success(sourceService.create(request));
    }

    @GetMapping("/sources")
    public ApiResponse<List<NotionSyncSourceResponse>> findSources() {
        return ApiResponse.success(sourceService.findAll());
    }

    @DeleteMapping("/sources/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSource(@PathVariable Long id) {
        sourceService.delete(id);
    }

    // 주기 실행을 기다리지 않고 바로 동기화한다.
    @PostMapping("/sync")
    public ApiResponse<List<NotionSyncResult>> sync() {
        if (!syncService.isEnabled()) {
            throw new InvalidRequestException("NOTION_TOKEN이 설정되지 않아 노션 동기화를 할 수 없습니다");
        }
        return ApiResponse.success(syncService.syncAll());
    }
}
