package com.junyoung.dashboard.domain.user.service;

import com.junyoung.dashboard.domain.user.dto.UserProfileRequest;
import com.junyoung.dashboard.domain.user.dto.UserProfileResponse;
import com.junyoung.dashboard.domain.user.entity.UserProfile;
import com.junyoung.dashboard.domain.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserProfileService {

    private static final String DEFAULT_DISPLAY_NAME = "사용자";

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserProfileResponse getProfile() {
        return UserProfileResponse.from(getOrCreate());
    }

    @Transactional
    public UserProfileResponse update(UserProfileRequest request) {
        UserProfile profile = getOrCreate();
        profile.update(request.displayName(), request.email(), request.bio());
        return UserProfileResponse.from(profile);
    }

    private UserProfile getOrCreate() {
        List<UserProfile> existing = userProfileRepository.findAll();
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return userProfileRepository.save(new UserProfile(DEFAULT_DISPLAY_NAME, null, null));
    }
}
