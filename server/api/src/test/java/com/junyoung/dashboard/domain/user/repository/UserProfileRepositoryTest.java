package com.junyoung.dashboard.domain.user.repository;

import com.junyoung.dashboard.domain.user.entity.UserProfile;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class UserProfileRepositoryTest {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        UserProfile saved = userProfileRepository.save(new UserProfile("사용자", null, null));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void startsEmptyBeforeAnyRowIsCreated() {
        assertThat(userProfileRepository.findAll()).isEmpty();
    }
}
