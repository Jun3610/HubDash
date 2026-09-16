package com.junyoung.dashboard.domain.user.service;

import com.junyoung.dashboard.domain.user.dto.UserSettingRequest;
import com.junyoung.dashboard.domain.user.dto.UserSettingResponse;
import com.junyoung.dashboard.domain.user.entity.UserSetting;
import com.junyoung.dashboard.domain.user.repository.UserSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSettingServiceTest {

    @Mock
    private UserSettingRepository userSettingRepository;

    private UserSettingService userSettingService;

    @BeforeEach
    void setUp() {
        userSettingService = new UserSettingService(userSettingRepository);
    }

    @Test
    void createsDefaultSettingWhenNoneExistsYet() {
        when(userSettingRepository.findAll()).thenReturn(List.of());
        when(userSettingRepository.save(any(UserSetting.class)))
                .thenReturn(new UserSetting("LIGHT", "ko", true));

        UserSettingResponse response = userSettingService.getSettings();

        assertThat(response.theme()).isEqualTo("LIGHT");
        assertThat(response.language()).isEqualTo("ko");
        assertThat(response.notificationEnabled()).isTrue();

        ArgumentCaptor<UserSetting> captor = ArgumentCaptor.forClass(UserSetting.class);
        verify(userSettingRepository).save(captor.capture());
        assertThat(captor.getValue().getTheme()).isEqualTo("LIGHT");
    }

    @Test
    void returnsExistingSettingWithoutCreatingANewOne() {
        UserSetting existing = new UserSetting("DARK", "en", false);
        when(userSettingRepository.findAll()).thenReturn(List.of(existing));

        UserSettingResponse response = userSettingService.getSettings();

        assertThat(response.theme()).isEqualTo("DARK");
        verify(userSettingRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void updateReflectsEveryField() {
        UserSetting existing = new UserSetting("LIGHT", "ko", true);
        when(userSettingRepository.findAll()).thenReturn(List.of(existing));

        UserSettingRequest request = new UserSettingRequest("DARK", "en", false);

        UserSettingResponse response = userSettingService.update(request);

        assertThat(response.theme()).isEqualTo("DARK");
        assertThat(response.language()).isEqualTo("en");
        assertThat(response.notificationEnabled()).isFalse();
    }
}
