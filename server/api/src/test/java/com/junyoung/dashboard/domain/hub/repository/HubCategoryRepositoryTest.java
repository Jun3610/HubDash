package com.junyoung.dashboard.domain.hub.repository;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
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
class HubCategoryRepositoryTest {

    @Autowired
    private HubCategoryRepository hubCategoryRepository;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        HubCategory saved = hubCategoryRepository.save(new HubCategory("CI/CD", "빌드 파이프라인 문서"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
