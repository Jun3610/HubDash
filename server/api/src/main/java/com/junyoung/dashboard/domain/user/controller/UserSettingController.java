package com.junyoung.dashboard.domain.user.controller;

import com.junyoung.dashboard.domain.user.dto.UserSettingRequest;
import com.junyoung.dashboard.domain.user.dto.UserSettingResponse;
import com.junyoung.dashboard.domain.user.service.UserSettingService;
import com.junyoung.dashboard.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/settings")
public class UserSettingController {

    private final UserSettingService userSettingService;

    public UserSettingController(UserSettingService userSettingService) {
        this.userSettingService = userSettingService;
    }

    @GetMapping
    public ApiResponse<UserSettingResponse> getSettings() {
        return ApiResponse.success(userSettingService.getSettings());
    }

    @PutMapping
    public ApiResponse<UserSettingResponse> update(@Valid @RequestBody UserSettingRequest request) {
        return ApiResponse.success(userSettingService.update(request));
    }
}
