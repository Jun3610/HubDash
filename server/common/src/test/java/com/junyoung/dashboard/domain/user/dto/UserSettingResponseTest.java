package com.junyoung.dashboard.domain.user.dto;

import com.junyoung.dashboard.domain.user.entity.UserSetting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserSettingResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        UserSetting userSetting = new UserSetting("DARK", "en", false);

        UserSettingResponse response = UserSettingResponse.from(userSetting);

        assertThat(response.theme()).isEqualTo("DARK");
        assertThat(response.language()).isEqualTo("en");
        assertThat(response.notificationEnabled()).isFalse();
    }
}
