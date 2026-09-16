package com.junyoung.dashboard.domain.user.controller;

import com.junyoung.dashboard.domain.user.dto.UserProfileRequest;
import com.junyoung.dashboard.domain.user.dto.UserProfileResponse;
import com.junyoung.dashboard.domain.user.service.UserProfileService;
import com.junyoung.dashboard.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ApiResponse<UserProfileResponse> getProfile() {
        return ApiResponse.success(userProfileService.getProfile());
    }

    @PutMapping
    public ApiResponse<UserProfileResponse> update(@Valid @RequestBody UserProfileRequest request) {
        return ApiResponse.success(userProfileService.update(request));
    }
}
