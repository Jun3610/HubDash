package com.junyoung.dashboard.domain.user.dto;

import com.junyoung.dashboard.domain.user.entity.UserProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        UserProfile userProfile = new UserProfile("박준영", "junp3610@example.com", "백엔드 개발자");

        UserProfileResponse response = UserProfileResponse.from(userProfile);

        assertThat(response.displayName()).isEqualTo("박준영");
        assertThat(response.email()).isEqualTo("junp3610@example.com");
        assertThat(response.bio()).isEqualTo("백엔드 개발자");
    }
}
