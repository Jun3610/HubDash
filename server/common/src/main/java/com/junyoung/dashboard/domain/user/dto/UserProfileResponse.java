package com.junyoung.dashboard.domain.user.dto;

import com.junyoung.dashboard.domain.user.entity.UserProfile;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String displayName,
        String email,
        String bio,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserProfileResponse from(UserProfile userProfile) {
        return new UserProfileResponse(
                userProfile.getId(),
                userProfile.getDisplayName(),
                userProfile.getEmail(),
                userProfile.getBio(),
                userProfile.getCreatedAt(),
                userProfile.getUpdatedAt()
        );
    }
}
