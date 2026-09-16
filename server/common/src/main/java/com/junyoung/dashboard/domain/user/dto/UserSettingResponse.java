package com.junyoung.dashboard.domain.user.dto;

import com.junyoung.dashboard.domain.user.entity.UserSetting;

import java.time.LocalDateTime;

public record UserSettingResponse(
        Long id,
        String theme,
        String language,
        Boolean notificationEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserSettingResponse from(UserSetting userSetting) {
        return new UserSettingResponse(
                userSetting.getId(),
                userSetting.getTheme(),
                userSetting.getLanguage(),
                userSetting.getNotificationEnabled(),
                userSetting.getCreatedAt(),
                userSetting.getUpdatedAt()
        );
    }
}
