package com.junyoung.dashboard.domain.integration.controller;

import com.junyoung.dashboard.domain.integration.dto.CalendarOption;
import com.junyoung.dashboard.domain.integration.dto.ICloudIntegrationRequest;
import com.junyoung.dashboard.domain.integration.dto.IntegrationResponse;
import com.junyoung.dashboard.domain.integration.dto.IntegrationSaveResponse;
import com.junyoung.dashboard.domain.integration.dto.IntegrationTestResponse;
import com.junyoung.dashboard.domain.integration.dto.NotionIntegrationRequest;
import com.junyoung.dashboard.domain.integration.entity.IntegrationProvider;
import com.junyoung.dashboard.domain.integration.service.IntegrationService;
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
import java.util.Locale;

/** 설정 화면의 연동(Integrations) — 노션 토큰, Apple ID·앱 암호, 가져올 캘린더 (이슈 #227) */
@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {

    private final IntegrationService service;

    public IntegrationController(IntegrationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<IntegrationResponse>> list() {
        return ApiResponse.success(service.list());
    }

    @PutMapping("/notion")
    public ApiResponse<IntegrationSaveResponse> putNotion(@Valid @RequestBody NotionIntegrationRequest request) {
        return ApiResponse.success(service.putNotion(request));
    }

    @PutMapping("/icloud")
    public ApiResponse<IntegrationSaveResponse> putICloud(@Valid @RequestBody ICloudIntegrationRequest request) {
        return ApiResponse.success(service.putICloud(request));
    }

    @GetMapping("/icloud/calendars")
    public ApiResponse<List<CalendarOption>> icloudCalendars() {
        return ApiResponse.success(service.icloudCalendars());
    }

    @PostMapping("/{provider}/test")
    public ApiResponse<IntegrationTestResponse> test(@PathVariable String provider) {
        return ApiResponse.success(service.test(parse(provider)));
    }

    @DeleteMapping("/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String provider) {
        service.delete(parse(provider));
    }

    // 주소는 소문자(/notion, /icloud)로 받는다
    private static IntegrationProvider parse(String provider) {
        try {
            return IntegrationProvider.valueOf(provider.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new com.junyoung.dashboard.global.exception.EntityNotFoundException("연동 " + provider + "는 없어요 (notion, icloud)");
        }
    }
}
