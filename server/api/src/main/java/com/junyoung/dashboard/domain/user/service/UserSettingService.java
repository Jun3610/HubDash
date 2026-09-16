package com.junyoung.dashboard.domain.user.service;

import com.junyoung.dashboard.domain.user.dto.UserSettingRequest;
import com.junyoung.dashboard.domain.user.dto.UserSettingResponse;
import com.junyoung.dashboard.domain.user.entity.UserSetting;
import com.junyoung.dashboard.domain.user.repository.UserSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserSettingService {

    private static final String DEFAULT_THEME = "LIGHT";
    private static final String DEFAULT_LANGUAGE = "ko";
    private static final boolean DEFAULT_NOTIFICATION_ENABLED = true;

    private final UserSettingRepository userSettingRepository;

    public UserSettingService(UserSettingRepository userSettingRepository) {
        this.userSettingRepository = userSettingRepository;
    }

    @Transactional
    public UserSettingResponse getSettings() {
        return UserSettingResponse.from(getOrCreate());
    }

    @Transactional
    public UserSettingResponse update(UserSettingRequest request) {
        UserSetting setting = getOrCreate();
        setting.update(request.theme(), request.language(), request.notificationEnabled());
        return UserSettingResponse.from(setting);
    }

    private UserSetting getOrCreate() {
        List<UserSetting> existing = userSettingRepository.findAll();
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return userSettingRepository.save(
                new UserSetting(DEFAULT_THEME, DEFAULT_LANGUAGE, DEFAULT_NOTIFICATION_ENABLED));
    }
}
