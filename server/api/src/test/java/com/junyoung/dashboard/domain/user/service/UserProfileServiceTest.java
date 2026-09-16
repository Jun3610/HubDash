package com.junyoung.dashboard.domain.user.service;

import com.junyoung.dashboard.domain.user.dto.UserProfileRequest;
import com.junyoung.dashboard.domain.user.dto.UserProfileResponse;
import com.junyoung.dashboard.domain.user.entity.UserProfile;
import com.junyoung.dashboard.domain.user.repository.UserProfileRepository;
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
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(userProfileRepository);
    }

    @Test
    void createsDefaultProfileWhenNoneExistsYet() {
        when(userProfileRepository.findAll()).thenReturn(List.of());
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenReturn(new UserProfile("사용자", null, null));

        UserProfileResponse response = userProfileService.getProfile();

        assertThat(response.displayName()).isEqualTo("사용자");

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getDisplayName()).isEqualTo("사용자");
    }

    @Test
    void returnsExistingProfileWithoutCreatingANewOne() {
        UserProfile existing = new UserProfile("박준영", "junp3610@example.com", "백엔드 개발자");
        when(userProfileRepository.findAll()).thenReturn(List.of(existing));

        UserProfileResponse response = userProfileService.getProfile();

        assertThat(response.displayName()).isEqualTo("박준영");
        verify(userProfileRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void updateReflectsEveryField() {
        UserProfile existing = new UserProfile("사용자", null, null);
        when(userProfileRepository.findAll()).thenReturn(List.of(existing));

        UserProfileRequest request = new UserProfileRequest("박준영", "junp3610@example.com", "백엔드 개발자");

        UserProfileResponse response = userProfileService.update(request);

        assertThat(response.displayName()).isEqualTo("박준영");
        assertThat(response.email()).isEqualTo("junp3610@example.com");
        assertThat(response.bio()).isEqualTo("백엔드 개발자");
    }
}
