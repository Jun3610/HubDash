package com.junyoung.dashboard.domain.hub.repository;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.entity.HubLink;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class HubLinkRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HubLinkRepository hubLinkRepository;

    @Test
    void findsLinksByCategoryId() {
        HubCategory category = entityManager.persistAndFlush(new HubCategory("CI/CD", null));
        entityManager.persistAndFlush(new HubLink(category, "Docker 문서", "https://example.com/docker", null));

        List<HubLink> links = hubLinkRepository.findByCategoryId(category.getId());

        assertThat(links).hasSize(1);
        assertThat(links.get(0).getTitle()).isEqualTo("Docker 문서");
    }
}
