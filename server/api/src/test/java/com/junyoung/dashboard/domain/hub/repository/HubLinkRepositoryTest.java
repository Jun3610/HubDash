package com.junyoung.dashboard.domain.hub.repository;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.entity.HubLink;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

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

        Page<HubLink> links = hubLinkRepository.findByCategoryId(category.getId(), Pageable.unpaged());

        assertThat(links.getContent()).hasSize(1);
        assertThat(links.getContent().get(0).getTitle()).isEqualTo("Docker 문서");
    }
}
